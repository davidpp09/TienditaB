package tiendita.api.venta;

import tiendita.api.comun.FormaPago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
import tiendita.api.caja.MovimientoCaja;
import tiendita.api.caja.TipoMovimientoCaja;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CobroTest {

    @Autowired VentaService ventas;
    @Autowired ProductoRepository productos;
    @Autowired MovimientoInventarioRepository kardex;
    @Autowired CajaService caja;

    private Producto producto(String nombre, Unidad unidad, String precio, String costo, String existencia) {
        Producto p = new Producto(null, nombre, unidad, new BigDecimal(precio));
        p.setCostoPromedio(new BigDecimal(costo));
        p.moverExistencia(new BigDecimal(existencia));
        return productos.save(p);
    }

    @Test
    void cobrarDescuentaElInventarioYEscribeElKardex() {
        Producto coca = producto("Coca 600ml", Unidad.PIEZA, "18.00", "13.5000", "10");

        Venta venta = ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(coca.getId(), new BigDecimal("2"))),
                FormaPago.EFECTIVO, new BigDecimal("50")), "test");

        assertThat(venta.getTotal()).isEqualByComparingTo("36.00");
        assertThat(venta.getCambio()).isEqualByComparingTo("14.00");
        assertThat(coca.getExistencia()).isEqualByComparingTo("8");

        List<MovimientoInventario> movs = kardex.findByProductoIdOrderByFechaHoraDescIdDesc(coca.getId());
        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).getTipo()).isEqualTo(TipoMovimiento.VENTA);
        assertThat(movs.get(0).getCantidad()).isEqualByComparingTo("-2");
        assertThat(movs.get(0).getExistenciaResultante()).isEqualByComparingTo("8");
        assertThat(movs.get(0).getReferenciaId()).isEqualTo(venta.getId());
    }

    /**
     * El corazón del sistema: el costo se congela en el renglón. Si después sube
     * el costo del producto, la utilidad de esta venta no puede moverse.
     */
    @Test
    void elCostoSeCongelaEnElRenglonDeLaVenta() {
        Producto atun = producto("Atún", Unidad.PIEZA, "21.00", "18.5000", "20");

        Venta venta = ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(atun.getId(), new BigDecimal("3"))),
                FormaPago.EFECTIVO, null), "test");

        assertThat(venta.getUtilidad()).isEqualByComparingTo("7.50");   // 63.00 − 55.50

        atun.setCostoPromedio(new BigDecimal("20.6000"));               // el proveedor subió
        productos.saveAndFlush(atun);

        assertThat(venta.getDetalles().get(0).getCostoUnitario()).isEqualByComparingTo("18.5000");
        assertThat(venta.getUtilidad()).isEqualByComparingTo("7.50");
    }

    @Test
    void elGranelSeVendeConDecimales() {
        Producto frijol = producto("Frijol a granel", Unidad.KILO, "32.00", "24.0000", "50");

        Venta venta = ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(frijol.getId(), new BigDecimal("1.250"))),
                FormaPago.EFECTIVO, null), "test");

        assertThat(venta.getTotal()).isEqualByComparingTo("40.00");
        assertThat(frijol.getExistencia()).isEqualByComparingTo("48.750");
    }

    @Test
    void unaPiezaNoSeVendeEnFracciones() {
        Producto coca = producto("Coca 600ml", Unidad.PIEZA, "18.00", "13.5000", "10");

        assertThatThrownBy(() -> ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(coca.getId(), new BigDecimal("1.5"))),
                FormaPago.EFECTIVO, null), "test"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("por pieza");
    }

    /** Si el conteo está mal, se cobra igual: la venta nunca se detiene. */
    @Test
    void sePuedeVenderAunqueLaExistenciaQuedeNegativa() {
        Producto vasos = producto("Vasos #10", Unidad.PIEZA, "45.00", "38.0000", "1");

        ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(vasos.getId(), new BigDecimal("3"))),
                FormaPago.EFECTIVO, null), "test");

        assertThat(vasos.getExistencia()).isEqualByComparingTo("-2");
    }

    @Test
    void noSeCobraSiElDineroRecibidoNoAlcanza() {
        Producto coca = producto("Coca 600ml", Unidad.PIEZA, "18.00", "13.5000", "10");

        assertThatThrownBy(() -> ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(coca.getId(), new BigDecimal("2"))),
                FormaPago.EFECTIVO, new BigDecimal("30")), "test"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("menor que el total");
    }

    /** Una venta con tarjeta no mete efectivo al cajón, así que no toca la caja. */
    @Test
    void laTarjetaNoMueveElCajon() {
        Producto coca = producto("Coca 600ml", Unidad.PIEZA, "18.00", "13.5000", "10");
        Long corte = caja.corteAbierto("test").getId();

        ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(coca.getId(), new BigDecimal("1"))),
                FormaPago.TARJETA, null), "test");

        List<MovimientoCaja> movimientos = caja.movimientosDelCorte(corte);
        assertThat(movimientos).noneMatch(m -> m.getTipo() == TipoMovimientoCaja.VENTA);
    }

    @Test
    void laVentaEnEfectivoEntraALaCaja() {
        Producto coca = producto("Coca 600ml", Unidad.PIEZA, "18.00", "13.5000", "10");
        Long corte = caja.corteAbierto("test").getId();

        ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(coca.getId(), new BigDecimal("2"))),
                FormaPago.EFECTIVO, new BigDecimal("50")), "test");

        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("36.00");
    }
}
