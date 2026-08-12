package tiendita.api.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.venta.FormaPago;
import tiendita.api.venta.Venta;
import tiendita.api.venta.VentaDTO;
import tiendita.api.venta.VentaService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code producto.existencia} es un caché del kardex. Este test es el que dice
 * si el caché sigue siendo fiel al libro: la suma de todos los movimientos tiene
 * que dar exactamente la existencia, pase lo que pase.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KardexCuadraTest {

    @Autowired ProductoService servicio;
    @Autowired ProductoRepository productos;
    @Autowired MovimientoInventarioRepository kardex;
    @Autowired VentaService ventas;

    @Test
    void laSumaDelKardexEsSiempreLaExistencia() {
        Producto p = new Producto("7501000111", "Frijol a granel", Unidad.KILO, new BigDecimal("32.00"));
        p.setCostoPromedio(new BigDecimal("24.0000"));
        productos.save(p);

        servicio.ajustarPorConteo(p.getId(), new BigDecimal("40.000"), "conteo inicial", "test");
        vender(p, "1.250");
        vender(p, "0.750");
        servicio.merma(p.getId(), new BigDecimal("2.000"), "se mojó el costal", "test");
        servicio.autoconsumo(p.getId(), new BigDecimal("1.000"), "para la casa", "test");
        Venta ultima = vender(p, "3.000");
        ventas.cancelar(ultima.getId(), "el cliente se arrepintió", "test");

        productos.flush();
        kardex.flush();

        // 40 − 1.25 − 0.75 − 2 − 1 − 3 + 3
        assertThat(p.getExistencia()).isEqualByComparingTo("35.000");
        assertThat(kardex.sumaCantidades(p.getId())).isEqualByComparingTo(p.getExistencia());
    }

    @Test
    void elConteoRecibeLaExistenciaRealNoLaDiferencia() {
        Producto p = productos.save(new Producto(null, "Vasos #10", Unidad.PIEZA, new BigDecimal("45.00")));
        p.moverExistencia(new BigDecimal("30"));

        servicio.ajustarPorConteo(p.getId(), new BigDecimal("27"), "conteo del lunes", "test");

        assertThat(p.getExistencia()).isEqualByComparingTo("27");
        assertThat(kardex.findByProductoIdOrderByFechaHoraDescIdDesc(p.getId()).get(0).getCantidad())
                .isEqualByComparingTo("-3");
    }

    private Venta vender(Producto p, String cantidad) {
        return ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(p.getId(), new BigDecimal(cantidad))),
                FormaPago.EFECTIVO, null), "test");
    }
}
