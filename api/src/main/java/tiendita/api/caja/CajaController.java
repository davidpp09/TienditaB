package tiendita.api.caja;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/caja")
public class CajaController {

    private final CajaService servicio;

    public CajaController(CajaService servicio) {
        this.servicio = servicio;
    }

    public record Apertura(@NotNull BigDecimal fondoInicial) {}
    public record Movimiento(@NotNull BigDecimal monto, String concepto, String categoria) {}
    public record Cierre(@NotNull BigDecimal contado, String notas) {}

    public record EstadoCaja(boolean abierta, Long corteId, LocalDate fecha, LocalDateTime abiertoEn,
                             BigDecimal fondoInicial, BigDecimal esperado) {}

    public record MovimientoVista(LocalDateTime fechaHora, TipoMovimientoCaja tipo, BigDecimal monto,
                                  String concepto, String categoriaGasto) {
        static MovimientoVista de(MovimientoCaja m) {
            return new MovimientoVista(m.getFechaHora(), m.getTipo(), m.getMonto(),
                    m.getConcepto(), m.getCategoriaGasto());
        }
    }

    public record CorteVista(Long id, LocalDate fecha, BigDecimal fondoInicial, BigDecimal totalVentas,
                             BigDecimal totalGastos, BigDecimal totalRetiros, BigDecimal esperado,
                             BigDecimal contado, BigDecimal diferencia, LocalDateTime cerradoEn) {
        static CorteVista de(CorteCaja c) {
            return new CorteVista(c.getId(), c.getFecha(), c.getFondoInicial(), c.getTotalVentas(),
                    c.getTotalGastos(), c.getTotalRetiros(), c.getEsperado(), c.getContado(),
                    c.getDiferencia(), c.getCerradoEn());
        }
    }

    /**
     * Consultar cómo está la caja NO la abre. La pantalla se refresca sola cada
     * tantos segundos; si esto abriera un corte, tener la pantalla encendida un
     * domingo cerrado inventaría cajas de días que nunca existieron.
     */
    @GetMapping
    public EstadoCaja estado() {
        return servicio.corteAbiertoSiHay()
                .map(c -> new EstadoCaja(true, c.getId(), c.getFecha(), c.getAbiertoEn(),
                        c.getFondoInicial(), servicio.esperadoEnCaja(c.getId())))
                .orElseGet(() -> new EstadoCaja(false, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /** Los movimientos del día en curso, para el detalle del corte. */
    @GetMapping("/movimientos")
    public List<MovimientoVista> movimientos() {
        return servicio.corteAbiertoSiHay()
                .map(c -> servicio.movimientosDelCorte(c.getId()).stream().map(MovimientoVista::de).toList())
                .orElseGet(List::of);
    }

    @PostMapping("/abrir")
    public CorteVista abrir(@RequestBody Apertura a) {
        return CorteVista.de(servicio.abrir(a.fondoInicial(), usuario()));
    }

    @PostMapping("/gasto")
    public void gasto(@RequestBody Movimiento m) {
        servicio.gasto(m.monto(), m.concepto(), m.categoria(), usuario());
    }

    @PostMapping("/retiro")
    public void retiro(@RequestBody Movimiento m) {
        servicio.retiro(m.monto(), m.concepto(), usuario());
    }

    @PostMapping("/deposito")
    public void deposito(@RequestBody Movimiento m) {
        servicio.deposito(m.monto(), m.concepto(), usuario());
    }

    @PostMapping("/cerrar")
    public CorteVista cerrar(@RequestBody Cierre c) {
        return CorteVista.de(servicio.cerrar(c.contado(), c.notas(), usuario()));
    }

    @GetMapping("/cortes")
    public List<CorteVista> cortes() {
        return servicio.ultimosCortes().stream().map(CorteVista::de).toList();
    }

    private String usuario() {
        return "mostrador";
    }
}
