package tiendita.api.venta;

import tiendita.api.comun.FormaPago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.kardex.MovimientoInventario;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.kardex.TipoMovimiento;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cancelar no borra: agrega el movimiento contrario. Es lo que permite que un
 * número de marzo siga siendo cierto en diciembre, y que un faltante no se
 * pueda hacer desaparecer.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CancelacionTest {

    @Autowired VentaService ventas;
    @Autowired VentaRepository repositorio;
    @Autowired ProductoRepository productos;
    @Autowired MovimientoInventarioRepository kardex;
    @Autowired CajaService caja;

    private Producto coca() {
        Producto p = new Producto(null, "Coca 600ml", Unidad.PIEZA, new BigDecimal("18.00"));
        p.setCostoPromedio(new BigDecimal("13.5000"));
        p.moverExistencia(new BigDecimal("10"));
        return productos.save(p);
    }

    private Venta cobrarDos(Producto p) {
        return ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(p.getId(), new BigDecimal("2"))),
                FormaPago.EFECTIVO, new BigDecimal("50")), "test");
    }

    @Test
    void cancelarDevuelveLaMercanciaYElDinero() {
        Producto coca = coca();
        Long corte = caja.corteAbierto("test").getId();
        Venta venta = cobrarDos(coca);

        ventas.cancelar(venta.getId(), "el cliente se arrepintió", "test");

        assertThat(coca.getExistencia()).isEqualByComparingTo("10");
        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("0.00");
        assertThat(venta.isCancelada()).isTrue();
        assertThat(venta.getMotivoCancelacion()).isEqualTo("el cliente se arrepintió");
    }

    @Test
    void laVentaCanceladaSigueExistiendoYSusMovimientosTambien() {
        Producto coca = coca();
        Venta venta = cobrarDos(coca);

        ventas.cancelar(venta.getId(), "error de captura", "test");

        assertThat(repositorio.findById(venta.getId())).isPresent();

        List<MovimientoInventario> movs = kardex.findByProductoIdOrderByFechaHoraDescIdDesc(coca.getId());
        assertThat(movs).hasSize(2);
        assertThat(movs).extracting(MovimientoInventario::getTipo)
                .containsExactlyInAnyOrder(TipoMovimiento.VENTA, TipoMovimiento.CANCELACION);
    }

    /**
     * Se devuelve al costo con el que salió, no al de hoy. Si no, cancelar una
     * venta vieja alteraría el valor del inventario por la puerta de atrás.
     */
    @Test
    void seDevuelveAlCostoConElQueSalio() {
        Producto coca = coca();
        Venta venta = cobrarDos(coca);
        coca.setCostoPromedio(new BigDecimal("15.0000"));
        productos.saveAndFlush(coca);

        ventas.cancelar(venta.getId(), "devolución", "test");

        MovimientoInventario devolucion = kardex.findByProductoIdOrderByFechaHoraDescIdDesc(coca.getId())
                .stream().filter(m -> m.getTipo() == TipoMovimiento.CANCELACION).findFirst().orElseThrow();
        assertThat(devolucion.getCostoUnitario()).isEqualByComparingTo("13.5000");
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        Venta venta = cobrarDos(coca());
        ventas.cancelar(venta.getId(), "primera", "test");

        assertThatThrownBy(() -> ventas.cancelar(venta.getId(), "segunda", "test"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya estaba cancelada");
    }
}
