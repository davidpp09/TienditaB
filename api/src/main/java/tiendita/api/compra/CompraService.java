package tiendita.api.compra;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
import tiendita.api.caja.TipoMovimientoCaja;
import tiendita.api.comun.FormaPago;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.kardex.MovimientoInventario;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.kardex.TipoMovimiento;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;
import tiendita.api.proveedor.Proveedor;
import tiendita.api.proveedor.ProveedorRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * La compra. Es el evento que le enseña al sistema cuánto cuestan las cosas:
 * hasta que se registra una, el costo de un producto dado de alta en el
 * mostrador es cero y su margen es una mentira del 100%.
 *
 * <p>Al guardar pasan cuatro cosas de golpe, todas dentro de la misma
 * transacción: entra la existencia, se recalcula el costo promedio, se escribe
 * el kardex, y sale el dinero de la caja si se pagó en efectivo.
 */
@Service
public class CompraService {

    private final CompraRepository compras;
    private final CompraDetalleRepository detalles;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final MovimientoInventarioRepository kardex;
    private final CajaService caja;

    public CompraService(CompraRepository compras, CompraDetalleRepository detalles,
                         ProveedorRepository proveedores, ProductoRepository productos,
                         MovimientoInventarioRepository kardex, CajaService caja) {
        this.compras = compras;
        this.detalles = detalles;
        this.proveedores = proveedores;
        this.productos = productos;
        this.kardex = kardex;
        this.caja = caja;
    }

    /** Lo que entró al inventario, para escribir el kardex ya con el id de la compra. */
    private record Entrada(Producto producto, BigDecimal cantidad, BigDecimal costo, BigDecimal resultante) {}

    @Transactional
    public CompraDTO.Vista registrar(CompraDTO.Registro datos, String usuario) {
        Proveedor proveedor = proveedores.findById(datos.proveedorId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el proveedor " + datos.proveedorId()));

        Compra compra = new Compra(proveedor,
                datos.fecha() == null ? LocalDate.now() : datos.fecha(),
                normalizarFolio(datos.folio()),
                datos.pagada() == null || datos.pagada(),
                datos.formaPago() == null ? FormaPago.EFECTIVO : datos.formaPago(),
                usuario);

        List<Entrada> entradas = new ArrayList<>();
        List<AvisoDeCosto> avisos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraDTO.Linea linea : datos.lineas()) {
            Producto p = productos.findByIdConBloqueo(linea.productoId())
                    .orElseThrow(() -> new EntityNotFoundException("No existe el producto " + linea.productoId()));
            BigDecimal cantidad = normalizarCantidad(p, linea.cantidad());
            BigDecimal costo = linea.costoUnitario().setScale(4, RoundingMode.HALF_UP);

            // Se pregunta ANTES de guardar nada, o el "último costo pagado"
            // sería el de esta misma compra.
            BigDecimal ultimoCosto = detalles.ultimoCostoPagado(p.getId()).orElse(null);

            p.setCostoPromedio(CostoPromedio.recalcular(p.getExistencia(), p.getCostoPromedio(), cantidad, costo));
            p.moverExistencia(cantidad);

            BigDecimal importe = costo.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            compra.agregar(new CompraDetalle(compra, p, cantidad, costo, importe));
            total = total.add(importe);
            entradas.add(new Entrada(p, cantidad, costo, p.getExistencia()));

            AvisoDeCosto aviso = AvisoDeCosto.siSubio(p, ultimoCosto, costo);
            if (aviso != null) {
                avisos.add(aviso);
            }
        }

        compra.totalizar(total);
        compras.saveAndFlush(compra);   // hace falta el id para referenciar el kardex

        for (Entrada e : entradas) {
            // En el kardex, una entrada se anota al costo que se PAGÓ, no al
            // promedio resultante: el kardex guarda lo que pasó, y lo que pasó
            // es que estas piezas costaron esto.
            kardex.save(new MovimientoInventario(e.producto(), TipoMovimiento.COMPRA, e.cantidad(),
                    e.costo(), e.resultante(), "COMPRA", compra.getId(), null, usuario));
        }

        if (compra.salioDelCajon()) {
            caja.registrar(TipoMovimientoCaja.COMPRA, total.negate(),
                            "Compra a " + proveedor.getNombre() + etiquetaDeFolio(compra), usuario)
                    .conReferencia("COMPRA", compra.getId());
        }

        return CompraDTO.Vista.de(compra, avisos);
    }

    @Transactional(readOnly = true)
    public CompraDTO.Vista vista(Long id) {
        return CompraDTO.Vista.de(porId(id), List.of());
    }

    /**
     * Como en las ventas: recorrer los renglones tiene que pasar DENTRO de la
     * transacción. Con {@code open-in-view=false}, armar la vista en el
     * controlador revienta con un 500.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO.Vista> ultimas() {
        return compras.findTop50ByOrderByFechaDescIdDesc().stream()
                .map(c -> CompraDTO.Vista.de(c, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompraDTO.Vista> deProveedor(Long proveedorId) {
        return compras.findByProveedorIdOrderByFechaDescIdDesc(proveedorId).stream()
                .map(c -> CompraDTO.Vista.de(c, List.of()))
                .toList();
    }

    public List<CompraDTO.PrecioPagado> historialDe(Long productoId) {
        return detalles.historialDe(productoId);
    }

    public Compra porId(Long id) {
        return compras.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la compra " + id));
    }

    /**
     * Misma regla que en la venta: una PIEZA no entra en fracciones. Una caja de
     * 24 refrescos se captura como 24 piezas, no como 1 caja — si no, la
     * existencia no se puede comparar contra lo que se vende.
     */
    private BigDecimal normalizarCantidad(Producto p, BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de " + p.getNombre() + " debe ser mayor que cero");
        }
        BigDecimal normalizada = cantidad.setScale(3, RoundingMode.HALF_UP);
        if (p.getUnidad() == Unidad.PIEZA && normalizada.stripTrailingZeros().scale() > 0) {
            throw new ReglaDeNegocioException(p.getNombre() + " se compra por pieza: la cantidad debe ser entera");
        }
        return normalizada;
    }

    private String normalizarFolio(String folio) {
        return folio == null || folio.isBlank() ? null : folio.trim();
    }

    private String etiquetaDeFolio(Compra compra) {
        return compra.getFolio() == null ? "" : " (folio " + compra.getFolio() + ")";
    }
}
