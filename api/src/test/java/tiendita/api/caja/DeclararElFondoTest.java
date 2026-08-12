package tiendita.api.caja;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;
import tiendita.api.venta.FormaPago;
import tiendita.api.venta.VentaDTO;
import tiendita.api.venta.VentaService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El día real no empieza declarando el fondo: empieza cobrando. Estos tests
 * fijan que el sistema aguante ese orden, que es el que de verdad ocurre.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeclararElFondoTest {

    @Autowired CajaService caja;
    @Autowired VentaService ventas;
    @Autowired ProductoRepository productos;

    private void venderCien() {
        Producto p = new Producto(null, "Producto de prueba", Unidad.PIEZA, new BigDecimal("100.00"));
        p.moverExistencia(new BigDecimal("10"));
        productos.save(p);
        ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(p.getId(), BigDecimal.ONE)), FormaPago.EFECTIVO, null), "test");
    }

    /**
     * Preguntar cómo está la caja no puede abrirla. La pantalla consulta esto
     * cada pocos segundos: si abriera, un domingo con la pantalla encendida
     * inventaría cajas de días que nunca existieron.
     */
    @Test
    void consultarLaCajaNoLaAbre() {
        assertThat(caja.corteAbiertoSiHay()).isEmpty();
        assertThat(caja.corteAbiertoSiHay()).isEmpty();
    }

    @Test
    void sePuedeDeclararElFondoDespuesDeQueLaVentaAbrioLaCaja() {
        venderCien();                                  // la caja se abre sola con fondo 0
        CorteCaja abierto = caja.corteAbiertoSiHay().orElseThrow();
        assertThat(abierto.getFondoInicial()).isEqualByComparingTo("0.00");

        caja.abrir(new BigDecimal("500.00"), "test");

        assertThat(abierto.getFondoInicial()).isEqualByComparingTo("500.00");
        // 500 del fondo + 100 que ya se había vendido
        assertThat(caja.esperadoEnCaja(abierto.getId())).isEqualByComparingTo("600.00");
    }

    @Test
    void noSePuedeDeclararElFondoDosVeces() {
        caja.abrir(new BigDecimal("500.00"), "test");

        assertThatThrownBy(() -> caja.abrir(new BigDecimal("300.00"), "test"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya tiene un fondo declarado");
    }

    @Test
    void elFondoNoPuedeSerNegativo() {
        assertThatThrownBy(() -> caja.abrir(new BigDecimal("-1"), "test"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no puede ser negativo");
    }
}
