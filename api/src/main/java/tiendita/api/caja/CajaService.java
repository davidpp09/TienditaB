package tiendita.api.caja;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.infra.ReglaDeNegocioException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private final CorteCajaRepository cortes;
    private final MovimientoCajaRepository movimientos;

    public CajaService(CorteCajaRepository cortes, MovimientoCajaRepository movimientos) {
        this.cortes = cortes;
        this.movimientos = movimientos;
    }

    /**
     * Devuelve el corte abierto, y si no hay ninguno lo abre con fondo cero.
     * <p>
     * Esto es deliberado: <b>nada puede impedir cobrar</b>. Si el cajero olvidó
     * declarar el fondo en la mañana, la venta se registra igual y el fondo se
     * corrige después. Un sistema que se niega a vender por un trámite es un
     * sistema que se apaga.
     */
    @Transactional
    public CorteCaja corteAbierto(String usuario) {
        return cortes.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc()
                .orElseGet(() -> abrirInterno(BigDecimal.ZERO, usuario));
    }

    /**
     * El corte abierto si lo hay, SIN abrir uno nuevo. Es lo que consulta la
     * pantalla: preguntar cómo está la caja no puede tener el efecto de abrirla.
     */
    public Optional<CorteCaja> corteAbiertoSiHay() {
        return cortes.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc();
    }

    /**
     * Declarar el fondo de caja de la mañana.
     * <p>
     * Si la caja ya se abrió sola —porque entró una venta antes de que nadie
     * declarara nada, que es lo que pasa casi todos los días— esto le pone el
     * fondo en lugar de negarse. Negarse obligaría a cerrar y reabrir la caja
     * para arreglar un trámite, y el dinero del día ya estaría adentro.
     */
    @Transactional
    public CorteCaja abrir(BigDecimal fondoInicial, String usuario) {
        if (fondoInicial == null || fondoInicial.signum() < 0) {
            throw new ReglaDeNegocioException("El fondo no puede ser negativo");
        }
        Optional<CorteCaja> abierto = corteAbiertoSiHay();
        if (abierto.isEmpty()) {
            return abrirInterno(fondoInicial, usuario);
        }

        CorteCaja corte = abierto.get();
        if (corte.getFondoInicial().signum() != 0) {
            throw new ReglaDeNegocioException("La caja ya tiene un fondo declarado de $"
                    + corte.getFondoInicial() + ". Haz el corte antes de abrir otra.");
        }
        corte.declararFondo(fondoInicial);
        if (fondoInicial.signum() != 0) {
            movimientos.save(new MovimientoCaja(TipoMovimientoCaja.FONDO, fondoInicial,
                    "Fondo de caja", corte, usuario));
        }
        return corte;
    }

    private CorteCaja abrirInterno(BigDecimal fondoInicial, String usuario) {
        CorteCaja corte = cortes.save(new CorteCaja(fondoInicial, usuario));
        if (fondoInicial.signum() != 0) {
            movimientos.save(new MovimientoCaja(TipoMovimientoCaja.FONDO, fondoInicial,
                    "Fondo de caja", corte, usuario));
        }
        return corte;
    }

    @Transactional
    public MovimientoCaja registrar(TipoMovimientoCaja tipo, BigDecimal monto, String concepto, String usuario) {
        return movimientos.save(new MovimientoCaja(tipo, monto, concepto, corteAbierto(usuario), usuario));
    }

    /** Salidas de dinero del cajón: se reciben en positivo y se guardan en negativo. */
    @Transactional
    public MovimientoCaja gasto(BigDecimal monto, String concepto, String categoria, String usuario) {
        exigirPositivo(monto);
        return movimientos.save(new MovimientoCaja(TipoMovimientoCaja.GASTO, monto.negate(), concepto,
                corteAbierto(usuario), usuario).conCategoriaGasto(categoria));
    }

    @Transactional
    public MovimientoCaja retiro(BigDecimal monto, String concepto, String usuario) {
        exigirPositivo(monto);
        return movimientos.save(new MovimientoCaja(TipoMovimientoCaja.RETIRO, monto.negate(), concepto,
                corteAbierto(usuario), usuario));
    }

    @Transactional
    public MovimientoCaja deposito(BigDecimal monto, String concepto, String usuario) {
        exigirPositivo(monto);
        return movimientos.save(new MovimientoCaja(TipoMovimientoCaja.DEPOSITO, monto, concepto,
                corteAbierto(usuario), usuario));
    }

    /** Lo que debería haber en el cajón ahora mismo. */
    public BigDecimal esperadoEnCaja(Long corteId) {
        return movimientos.sumaDelCorte(corteId);
    }

    @Transactional
    public CorteCaja cerrar(BigDecimal contado, String notas, String usuario) {
        CorteCaja corte = cortes.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc()
                .orElseThrow(() -> new ReglaDeNegocioException("No hay ninguna caja abierta que cerrar"));
        if (contado == null || contado.signum() < 0) {
            throw new ReglaDeNegocioException("Lo contado no puede ser negativo");
        }
        BigDecimal ventas  = movimientos.sumaDelCortePorTipo(corte.getId(), TipoMovimientoCaja.VENTA);
        BigDecimal gastos  = movimientos.sumaDelCortePorTipo(corte.getId(), TipoMovimientoCaja.GASTO).abs();
        BigDecimal retiros = movimientos.sumaDelCortePorTipo(corte.getId(), TipoMovimientoCaja.RETIRO).abs();
        BigDecimal esperado = esperadoEnCaja(corte.getId());
        corte.cerrar(ventas, gastos, retiros, esperado, contado, notas);
        return corte;
    }

    public List<MovimientoCaja> movimientosDelCorte(Long corteId) {
        return movimientos.findByCorteIdOrderByFechaHoraAscIdAsc(corteId);
    }

    public List<CorteCaja> ultimosCortes() {
        return cortes.findTop30ByCerradoEnIsNotNullOrderByFechaDescIdDesc();
    }

    private void exigirPositivo(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto debe ser mayor que cero");
        }
    }
}
