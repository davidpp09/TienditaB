package tiendita.api.venta;

import tiendita.api.comun.FormaPago;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tiendita.api.caja.CajaService;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Este test NO lleva {@code @Transactional}, y esa es toda su razón de existir.
 * <p>
 * Los demás tests corren dentro de una transacción abierta, así que la colección
 * de renglones siempre está disponible y el ticket se arma sin problema. En el
 * servidor real no es así: con {@code open-in-view=false} la sesión se cierra al
 * salir del servicio, y armar el ticket en el controlador devolvía HTTP 500.
 * Se descubrió probando la API con curl, no con los tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketFueraDeTransaccionTest {

    @Autowired VentaService ventas;
    @Autowired ProductoRepository productos;
    @Autowired CajaService caja;

    /**
     * Al no haber transacción, lo que este test escribe SE QUEDA. Cobrar deja una
     * caja abierta, y una caja abierta que sobrevive al test hace fallar a los
     * demás (`abrir` se niega, y el esperado arrastra ventas ajenas). Cerrarla
     * aquí es la contrapartida de haber renunciado al rollback.
     */
    @AfterEach
    void cerrarLaCajaQueDejeAbierta() {
        caja.cerrar(caja.esperadoEnCaja(caja.corteAbierto("test").getId()), "cierre del test", "test");
    }

    @Test
    void elTicketSeArmaFueraDeLaTransaccionQueCobro() {
        Producto p = new Producto(null, "Producto de prueba del ticket", Unidad.PIEZA, new BigDecimal("18.00"));
        p.setCostoPromedio(new BigDecimal("13.5000"));
        p.moverExistencia(new BigDecimal("10"));
        productos.save(p);

        Venta venta = ventas.cobrar(new VentaDTO.Cobro(
                List.of(new VentaDTO.Linea(p.getId(), new BigDecimal("2"))),
                FormaPago.EFECTIVO, new BigDecimal("50")), "test");

        String ticket = ventas.ticket(venta.getId());

        assertThat(ticket).contains("Producto de prueba del ticket").contains("36.00");
        assertThat(ventas.vista(venta.getId()).lineas()).hasSize(1);

        // Spring lee los .properties en ISO-8859-1: el pie escrito con un "¡"
        // literal salía impreso como "Â¡Gracias". Va escapado en el properties, y
        // esta línea es la que se entera si alguien lo "arregla" de vuelta.
        assertThat(ticket).contains("¡Gracias por su compra!");
    }
}
