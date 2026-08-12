package tiendita.api.venta;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El ticket sale en papel de 80 mm: 48 columnas y ni una más. Un renglón que se
 * pase no se ve mal, se corta — y el importe cortado es justo el dato que el
 * cliente reclama.
 */
class TicketTest {

    private final TicketService tickets = new TicketService("LA TIENDITA", "¡Gracias por su compra!");

    @Test
    void ningunRenglonSePasaDelAnchoDelPapel() {
        String ticket = tickets.armar(VentaDePrueba.conNombreLargo());

        assertThat(ticket.lines()).allSatisfy(renglon ->
                assertThat(renglon.length()).isLessThanOrEqualTo(TicketService.ANCHO));
    }

    @Test
    void elImporteNuncaSeCortaAunqueElNombreSeaLarguisimo() {
        String ticket = tickets.armar(VentaDePrueba.conNombreLargo());

        assertThat(ticket).contains("1234.00");
    }

    @Test
    void elGranelSeImprimeConDecimalesYLaPiezaSinEllos() {
        String ticket = tickets.armar(VentaDePrueba.mixta());

        assertThat(ticket).contains("1.250 Frijol a granel");
        assertThat(ticket).contains("2 Coca 600ml");
    }

    @Test
    void laVentaCanceladaLoDiceEnElTicket() {
        Venta venta = VentaDePrueba.mixta();
        venta.cancelar("prueba");

        assertThat(tickets.armar(venta)).contains("*** CANCELADA ***");
    }

    @Test
    void elCambioApareceCuandoSePagaEnEfectivo() {
        Venta venta = VentaDePrueba.mixta();
        venta.totalizar(new BigDecimal("76.00"), new BigDecimal("55.00"));

        String ticket = tickets.armar(venta);

        assertThat(ticket).contains("TOTAL").contains("76.00");
        assertThat(ticket).contains("CAMBIO").contains("24.00");
    }
}
