package tiendita.api.venta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Arma el ticket como texto plano de ancho fijo, que es lo que entiende una
 * impresora térmica en modo ESC/POS.
 * <p>
 * El ticket impreso no es un adorno: es el <b>respaldo de papel del día</b>. Si
 * la Pi se apaga y algo se pierde, con los tickets se reconstruye.
 * <p>
 * Todavía no se manda a ninguna impresora — no hay impresora. Este servicio
 * produce el texto y se prueba solo; conectarlo a la cola de CUPS es un cambio
 * de una línea cuando el aparato exista.
 */
@Service
public class TicketService {

    /** 48 columnas: papel de 80 mm con la fuente A de ESC/POS. */
    static final int ANCHO = 48;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String nombreTienda;
    private final String pieDeTicket;

    public TicketService(@Value("${tienda.nombre:LA TIENDITA}") String nombreTienda,
                         @Value("${tienda.pie-ticket:¡Gracias por su compra!}") String pieDeTicket) {
        this.nombreTienda = nombreTienda;
        this.pieDeTicket = pieDeTicket;
    }

    public String armar(Venta venta) {
        StringBuilder t = new StringBuilder();
        t.append(centrado(nombreTienda)).append('\n');
        t.append(linea()).append('\n');
        t.append(izquierdaYDerecha(venta.getFolio(), venta.getFechaHora().format(FECHA))).append('\n');
        if (venta.isCancelada()) {
            t.append(centrado("*** CANCELADA ***")).append('\n');
        }
        t.append(linea()).append('\n');

        for (VentaDetalle d : venta.getDetalles()) {
            t.append(recortar(cantidad(d) + " " + d.getDescripcion())).append('\n');
            t.append(izquierdaYDerecha("   x " + pesos(d.getPrecioUnitario()), pesos(d.getImporte()))).append('\n');
        }

        t.append(linea()).append('\n');
        t.append(izquierdaYDerecha("TOTAL", "$" + pesos(venta.getTotal()))).append('\n');
        if (venta.getRecibido() != null) {
            t.append(izquierdaYDerecha(venta.getFormaPago().name(), "$" + pesos(venta.getRecibido()))).append('\n');
        }
        if (venta.getCambio() != null) {
            t.append(izquierdaYDerecha("CAMBIO", "$" + pesos(venta.getCambio()))).append('\n');
        }
        t.append(linea()).append('\n');
        t.append(centrado(pieDeTicket)).append('\n');
        return t.toString();
    }

    /** El granel se imprime con decimales; las piezas, sin ellos. */
    private String cantidad(VentaDetalle d) {
        BigDecimal c = d.getCantidad();
        return c.stripTrailingZeros().scale() <= 0
                ? c.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : c.toPlainString();
    }

    private String pesos(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String linea() {
        return "-".repeat(ANCHO);
    }

    /** El papel no da de sí: lo que no cabe se corta aquí, no en la impresora. */
    private String recortar(String texto) {
        return texto.length() <= ANCHO ? texto : texto.substring(0, ANCHO);
    }

    private String centrado(String texto) {
        if (texto.length() >= ANCHO) {
            return texto.substring(0, ANCHO);
        }
        return " ".repeat((ANCHO - texto.length()) / 2) + texto;
    }

    /**
     * Pega el texto de la izquierda con el de la derecha rellenando en medio.
     * Si no caben, gana la derecha: el importe nunca se corta, el nombre sí.
     */
    private String izquierdaYDerecha(String izquierda, String derecha) {
        int espacio = ANCHO - derecha.length();
        if (izquierda.length() > espacio - 1) {
            izquierda = izquierda.substring(0, Math.max(0, espacio - 1));
        }
        return izquierda + " ".repeat(Math.max(1, espacio - izquierda.length())) + derecha;
    }
}
