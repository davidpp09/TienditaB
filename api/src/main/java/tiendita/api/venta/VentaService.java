package tiendita.api.venta;

import tiendita.api.comun.FormaPago;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
import tiendita.api.caja.TipoMovimientoCaja;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.kardex.MovimientoInventario;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.kardex.TipoMovimiento;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El cobro. Todo lo demás del sistema existe para que este método pueda escribir
 * la verdad: qué salió del inventario, cuánto costaba y cuánto dinero entró.
 */
@Service
public class VentaService {

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final MovimientoInventarioRepository kardex;
    private final CajaService caja;
    private final TicketService tickets;

    public VentaService(VentaRepository ventas, ProductoRepository productos,
                        MovimientoInventarioRepository kardex, CajaService caja,
                        TicketService tickets) {
        this.ventas = ventas;
        this.productos = productos;
        this.kardex = kardex;
        this.caja = caja;
        this.tickets = tickets;
    }

    /** Lo que se movió del inventario, para escribir el kardex ya con el id de la venta. */
    private record Salida(Producto producto, BigDecimal cantidadConSigno, BigDecimal costo, BigDecimal resultante) {}

    @Transactional
    public Venta cobrar(VentaDTO.Cobro cobro, String usuario) {
        Venta venta = new Venta(cobro.formaPago(), cobro.recibido(), usuario);
        List<Salida> salidas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal costoTotal = BigDecimal.ZERO;

        for (VentaDTO.Linea linea : cobro.lineas()) {
            Producto p = productos.findByIdConBloqueo(linea.productoId())
                    .orElseThrow(() -> new EntityNotFoundException("No existe el producto " + linea.productoId()));
            BigDecimal cantidad = normalizarCantidad(p, linea.cantidad());

            BigDecimal precio = p.getPrecioVenta();
            BigDecimal costo = p.getCostoPromedio();
            BigDecimal importe = precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            venta.agregar(new VentaDetalle(venta, p, cantidad, precio, costo, importe));
            total = total.add(importe);
            costoTotal = costoTotal.add(costo.multiply(cantidad));

            // La existencia se mueve aunque quede en negativo: la venta nunca se
            // detiene por un conteo mal hecho. El negativo queda a la vista.
            p.moverExistencia(cantidad.negate());
            salidas.add(new Salida(p, cantidad.negate(), costo, p.getExistencia()));
        }

        if (cobro.formaPago().mueveElCajon() && cobro.recibido() != null
                && cobro.recibido().compareTo(total) < 0) {
            throw new ReglaDeNegocioException("Lo recibido ($" + cobro.recibido() + ") es menor que el total ($" + total + ")");
        }

        venta.totalizar(total, costoTotal.setScale(2, RoundingMode.HALF_UP));
        ventas.saveAndFlush(venta);   // hace falta el id para referenciar el kardex

        for (Salida s : salidas) {
            kardex.save(new MovimientoInventario(s.producto(), TipoMovimiento.VENTA, s.cantidadConSigno(),
                    s.costo(), s.resultante(), "VENTA", venta.getId(), null, usuario));
        }

        if (venta.getFormaPago().mueveElCajon()) {
            caja.registrar(TipoMovimientoCaja.VENTA, venta.getTotal(),
                    "Venta " + venta.getFolio(), usuario)
                    .conReferencia("VENTA", venta.getId());
        }
        return venta;
    }

    /**
     * Cancelar no borra: devuelve la mercancía al inventario con movimientos
     * CANCELACION, saca el dinero de la caja y marca la venta. El renglón
     * original se queda donde está, para siempre.
     */
    @Transactional
    public Venta cancelar(Long ventaId, String motivo, String usuario) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("No existe la venta " + ventaId));
        if (venta.isCancelada()) {
            throw new ReglaDeNegocioException("La venta " + venta.getFolio() + " ya estaba cancelada");
        }

        for (VentaDetalle d : venta.getDetalles()) {
            Producto p = productos.findByIdConBloqueo(d.getProducto().getId()).orElseThrow();
            p.moverExistencia(d.getCantidad());
            // Se devuelve al costo con el que salió, no al costo de hoy: si no,
            // cancelar una venta vieja alteraría el valor del inventario.
            kardex.save(new MovimientoInventario(p, TipoMovimiento.CANCELACION, d.getCantidad(),
                    d.getCostoUnitario(), p.getExistencia(), "VENTA", venta.getId(), motivo, usuario));
        }

        if (venta.getFormaPago().mueveElCajon()) {
            caja.registrar(TipoMovimientoCaja.VENTA, venta.getTotal().negate(),
                    "Cancelación de " + venta.getFolio(), usuario)
                    .conReferencia("VENTA", venta.getId());
        }

        venta.cancelar(motivo);
        return venta;
    }

    public Venta porId(Long id) {
        return ventas.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la venta " + id));
    }

    /**
     * Las lecturas que recorren los renglones tienen que ocurrir DENTRO de una
     * transacción. Con {@code open-in-view=false} (que es lo correcto), la sesión
     * se cierra al salir del servicio, y armar el ticket en el controlador
     * reventaba con un 500. Lo fija TicketFueraDeTransaccionTest.
     */
    @Transactional(readOnly = true)
    public VentaDTO.Vista vista(Long id) {
        return VentaDTO.Vista.de(porId(id));
    }

    @Transactional(readOnly = true)
    public List<VentaDTO.Vista> ultimas() {
        return ventas.findTop50ByOrderByIdDesc().stream().map(VentaDTO.Vista::de).toList();
    }

    @Transactional(readOnly = true)
    public String ticket(Long id) {
        return tickets.armar(porId(id));
    }

    public ResumenVentas resumenDelDia(LocalDate dia) {
        return ventas.resumen(dia.atStartOfDay(), LocalDateTime.of(dia, LocalTime.MAX));
    }

    /**
     * Una PIEZA no se vende en fracciones: si el lector escanea dos veces, son 2,
     * no 1.5. El granel sí, con tres decimales (1.250 kg de frijol).
     */
    private BigDecimal normalizarCantidad(Producto p, BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de " + p.getNombre() + " debe ser mayor que cero");
        }
        BigDecimal normalizada = cantidad.setScale(3, RoundingMode.HALF_UP);
        if (p.getUnidad() == Unidad.PIEZA && normalizada.stripTrailingZeros().scale() > 0) {
            throw new ReglaDeNegocioException(p.getNombre() + " se vende por pieza: la cantidad debe ser entera");
        }
        return normalizada;
    }
}
