package tiendita.api.compra;

import tiendita.api.producto.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * El aviso de que un producto subió de costo, con el precio al que hay que
 * ponerlo para no perder el margen.
 *
 * <p>Esta es la función que paga el proyecto. Sin ella, el proveedor sube el
 * costo, el precio de venta se queda donde estaba, y el producto se vende
 * meses a pérdida sin que nadie lo note: el dinero entra a la caja todos los
 * días y la caja no distingue una venta buena de una mala.
 *
 * <p><b>El aviso no cambia el precio.</b> Solo lo sugiere. Subir un precio es
 * una decisión de David — a veces se aguanta el golpe a propósito para no
 * espantar al cliente — y el sistema no la toma por él.
 *
 * <h2>Qué es "margen" aquí</h2>
 * Margen <b>sobre el precio de venta</b>: de cada $100 que cobro, cuántos me
 * quedan después del costo. Es la medida estándar del comercio y es la que hace
 * que "margen" y "descuento" se puedan comparar.
 *
 * <p>La otra forma de contarlo es sobre el costo ("le gano 30% a lo que me
 * cuesta"), y da números más altos para la misma ganancia. Se eligió una sola
 * y se usa en todo el sistema, porque mezclarlas es de donde salen los precios
 * mal puestos.
 */
public record AvisoDeCosto(
        Long productoId,
        String producto,
        BigDecimal costoAnterior,
        BigDecimal costoNuevo,
        /** Cuánto subió el costo, en por ciento. */
        BigDecimal subioPorciento,
        BigDecimal precioActual,
        /** El margen que dejaba antes de que subiera el costo. */
        BigDecimal margenAnterior,
        /** El que deja ahora, si el precio se queda como está. */
        BigDecimal margenNuevo,
        /** A cuánto hay que ponerlo para volver al margen de antes. */
        BigDecimal precioSugerido,
        String mensaje
) {

    private static final int DECIMALES = 1;

    /**
     * Arma el aviso, o devuelve {@code null} si no hay nada que avisar.
     *
     * @param costoAnterior lo último que se pagó por este producto en una compra
     *                      anterior, o {@code null} si es la primera vez
     * @param costoNuevo    lo que trae la factura de hoy
     *
     * <p>Se compara contra el último costo pagado y no contra el costo promedio
     * del producto, a propósito. El promedio viene mezclado con lo que quedaba
     * en la bodega a precio viejo, y esa mezcla se va a acabar; lo que va a
     * costar reponer el producto mañana es lo de la factura de hoy. Poner el
     * precio sobre el promedio deja el precio corto en cuanto se termina lo
     * barato, y obliga a subirlo otra vez.
     *
     * <p>No se avisa cuando el costo bajó o se quedó igual (eso es una buena
     * noticia, no una alarma), ni en la primera compra de un producto: pasar de
     * "nunca lo había comprado" a un costo real no es un aumento, y avisarlo
     * llenaría de ruido el alta de cada producto nuevo.
     */
    static AvisoDeCosto siSubio(Producto p, BigDecimal costoAnterior, BigDecimal costoNuevo) {
        if (costoAnterior == null || costoAnterior.signum() <= 0
                || costoNuevo.compareTo(costoAnterior) <= 0) {
            return null;
        }

        BigDecimal precio = p.getPrecioVenta();
        BigDecimal subio = porciento(costoNuevo.subtract(costoAnterior), costoAnterior);
        BigDecimal margenAnterior = margen(precio, costoAnterior);
        BigDecimal margenNuevo = margen(precio, costoNuevo);
        BigDecimal sugerido = precioParaConservar(margenAnterior, costoNuevo);

        return new AvisoDeCosto(p.getId(), p.getNombre(),
                costoAnterior.setScale(2, RoundingMode.HALF_UP),
                costoNuevo.setScale(2, RoundingMode.HALF_UP),
                subio, precio, margenAnterior, margenNuevo, sugerido,
                redactar(p.getNombre(), costoAnterior, costoNuevo, subio, precio,
                        margenAnterior, margenNuevo, sugerido));
    }

    /** Margen sobre el precio de venta, en por ciento. */
    private static BigDecimal margen(BigDecimal precio, BigDecimal costo) {
        if (precio == null || precio.signum() <= 0) {
            return null;   // un producto sin precio no tiene margen que calcular
        }
        return porciento(precio.subtract(costo), precio);
    }

    /**
     * El precio que devuelve el margen que había antes.
     * {@code precio = costo / (1 − margen)}.
     */
    private static BigDecimal precioParaConservar(BigDecimal margenAnterior, BigDecimal costoNuevo) {
        if (margenAnterior == null || margenAnterior.signum() <= 0) {
            // Ya se vendía al costo o a pérdida: no hay margen que conservar.
            // Lo menos malo que se puede sugerir es no perder dinero.
            return costoNuevo.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fraccion = BigDecimal.ONE.subtract(margenAnterior.movePointLeft(2));
        return costoNuevo.divide(fraccion, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal porciento(BigDecimal parte, BigDecimal total) {
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, DECIMALES, RoundingMode.HALF_UP);
    }

    private static String redactar(String nombre, BigDecimal costoAnterior, BigDecimal costoNuevo,
                                   BigDecimal subio, BigDecimal precio, BigDecimal margenAnterior,
                                   BigDecimal margenNuevo, BigDecimal sugerido) {
        String encabezado = "%s pasó de $%s a $%s (+%s%%)."
                .formatted(nombre, dinero(costoAnterior), dinero(costoNuevo), subio);

        if (margenNuevo == null) {
            return encabezado + " No tiene precio de venta puesto.";
        }
        if (margenNuevo.signum() <= 0) {
            return encabezado + " A tu precio de $%s lo estás vendiendo a pérdida. Para volver a tu %s%% de margen: $%s."
                    .formatted(dinero(precio), margenAnterior, sugerido);
        }
        return encabezado + " A tu precio de $%s te queda %s%% de margen, de %s%% que tenías. Para conservarlo: $%s."
                .formatted(dinero(precio), margenNuevo, margenAnterior, sugerido);
    }

    private static String dinero(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
