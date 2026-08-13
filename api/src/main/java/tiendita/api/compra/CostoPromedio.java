package tiendita.api.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * El costo promedio ponderado móvil. Está aparte, sin base de datos ni Spring,
 * porque es la única aritmética del sistema de la que dependen todos los
 * números de dinero: si esta fórmula está mal, todos los reportes mienten y
 * nada más lo delata.
 *
 * <pre>
 *   costo_nuevo = (existencia × costo_actual + cantidad × costo_de_compra)
 *                 ─────────────────────────────────────────────────────────
 *                            existencia + cantidad
 * </pre>
 *
 * Se guarda con 4 decimales: es una división que se arrastra compra tras
 * compra, y redondear a centavos en cada paso corre el costo.
 */
final class CostoPromedio {

    /** Los mismos 4 decimales que la columna `producto.costo_promedio`. */
    private static final int DECIMALES = 4;

    private CostoPromedio() {}

    /**
     * @param existenciaActual la que hay ANTES de meter la compra; puede venir
     *                         en cero o en negativo si se vendió de más
     * @param costoActual      el promedio que traía el producto
     * @param cantidad         lo que entra, siempre positivo
     * @param costoDeCompra    lo que se pagó por unidad esta vez
     */
    static BigDecimal recalcular(BigDecimal existenciaActual, BigDecimal costoActual,
                                 BigDecimal cantidad, BigDecimal costoDeCompra) {

        // Dos casos en los que promediar sería inventar un número:
        //
        //  1. No hay existencia (o está en negativo porque se vendió de más).
        //     La fórmula se rompe: el divisor puede quedar en cero, y ponderar
        //     contra una existencia negativa da un costo negativo.
        //
        //  2. El costo actual es cero. Pasa siempre al principio: el producto
        //     se dio de alta en el mostrador sin costo, o entró por el conteo
        //     inicial. Esas piezas SÍ costaron algo, solo que nadie lo anotó.
        //     Promediarlas como si hubieran salido gratis rebaja el costo de
        //     todo el lote y hace que el margen se vea mejor de lo que es.
        //
        // En ambos, lo honesto es adoptar el único costo que sí conocemos: el
        // que acabamos de pagar.
        if (existenciaActual.signum() <= 0 || costoActual.signum() == 0) {
            return costoDeCompra.setScale(DECIMALES, RoundingMode.HALF_UP);
        }

        BigDecimal valorAnterior = existenciaActual.multiply(costoActual);
        BigDecimal valorQueEntra = cantidad.multiply(costoDeCompra);
        BigDecimal piezas = existenciaActual.add(cantidad);

        return valorAnterior.add(valorQueEntra).divide(piezas, DECIMALES, RoundingMode.HALF_UP);
    }
}
