package tiendita.api.caja;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;
import tiendita.api.venta.FormaPago;
import tiendita.api.venta.VentaDTO;
import tiendita.api.venta.VentaService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CorteDeCajaTest {

    @Autowired CajaService caja;
    @Autowired VentaService ventas;
    @Autowired ProductoRepository productos;

    private Producto producto(String precio, String existencia) {
        Producto p = new Producto(null, "Producto de prueba", Unidad.PIEZA, new BigDecimal(precio));
        p.setCostoPromedio(new BigDecimal("10.0000"));
        p.moverExistencia(new BigDecimal(existencia));
        return productos.save(p);
    }

    private void vender(Producto p, String cantidad, FormaPago forma) {
        ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(p.getId(), new BigDecimal(cantidad))), forma, null), "test");
    }

    @Test
    void elEsperadoEsFondoMasVentasEnEfectivoMenosGastosYRetiros() {
        CorteCaja corte = caja.abrir(new BigDecimal("500.00"), "test");
        Producto p = producto("100.00", "50");

        vender(p, "3", FormaPago.EFECTIVO);                 // +300
        vender(p, "2", FormaPago.TARJETA);                  //  no toca el cajón
        caja.gasto(new BigDecimal("120.00"), "Luz", "servicios", "test");
        caja.retiro(new BigDecimal("200.00"), "David", "test");

        // 500 + 300 − 120 − 200
        assertThat(caja.esperadoEnCaja(corte.getId())).isEqualByComparingTo("480.00");
    }

    @Test
    void elCorteGuardaLaDiferenciaAunqueFalteDinero() {
        caja.abrir(new BigDecimal("500.00"), "test");
        Producto p = producto("100.00", "50");
        vender(p, "1", FormaPago.EFECTIVO);

        CorteCaja cerrado = caja.cerrar(new BigDecimal("580.00"), "faltaron 20", "test");

        assertThat(cerrado.getEsperado()).isEqualByComparingTo("600.00");
        assertThat(cerrado.getContado()).isEqualByComparingTo("580.00");
        assertThat(cerrado.getDiferencia()).isEqualByComparingTo("-20.00");
        assertThat(cerrado.getTotalVentas()).isEqualByComparingTo("100.00");
        assertThat(cerrado.estaAbierto()).isFalse();
    }

    /**
     * Si nadie declaró el fondo en la mañana, la venta NO se puede detener: se
     * abre un corte con fondo cero y el dinero queda registrado igual.
     */
    @Test
    void seCobraAunqueNadieHayaAbiertoLaCaja() {
        Producto p = producto("100.00", "50");

        vender(p, "1", FormaPago.EFECTIVO);

        CorteCaja corte = caja.corteAbierto("test");
        assertThat(corte.getFondoInicial()).isEqualByComparingTo("0.00");
        assertThat(caja.esperadoEnCaja(corte.getId())).isEqualByComparingTo("100.00");
    }
}
