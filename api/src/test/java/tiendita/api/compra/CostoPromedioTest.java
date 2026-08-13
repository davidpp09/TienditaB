package tiendita.api.compra;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aritmética pura, sin base de datos ni Spring. De esta fórmula dependen todos
 * los números de dinero del sistema, así que conviene poder probarla sola y en
 * milisegundos.
 */
class CostoPromedioTest {

    private BigDecimal recalcular(String existencia, String costoActual, String cantidad, String costoDeCompra) {
        return CostoPromedio.recalcular(new BigDecimal(existencia), new BigDecimal(costoActual),
                new BigDecimal(cantidad), new BigDecimal(costoDeCompra));
    }

    /** El ejemplo del handbook: 10 piezas a $18.00, entran 20 a $20.60. */
    @Test
    void elPromedioSePonderaPorLasPiezasDeCadaLote() {
        assertThat(recalcular("10", "18.00", "20", "20.60")).isEqualByComparingTo("19.7333");
    }

    @Test
    void sinExistenciaElCostoEsElDeLaCompra() {
        assertThat(recalcular("0", "18.00", "20", "20.60")).isEqualByComparingTo("20.60");
    }

    /**
     * La existencia queda en negativo cuando se vendió algo que el sistema creía
     * agotado. Ponderar contra un número negativo daría un costo negativo, que
     * es peor que no saber.
     */
    @Test
    void conExistenciaNegativaElCostoEsElDeLaCompra() {
        assertThat(recalcular("-3", "18.00", "20", "20.60")).isEqualByComparingTo("20.60");
    }

    /**
     * El caso del arranque, y el que más daño hace si se hace mal: el producto
     * se dio de alta en el mostrador sin costo, o entró por el conteo inicial.
     * Esas piezas costaron algo; nadie lo anotó. Promediarlas como si hubieran
     * salido gratis rebaja el costo del lote entero y hace que el margen se vea
     * mejor de lo que es, que es exactamente el error que este sistema existe
     * para evitar.
     */
    @Test
    void loQueNuncaTuvoCostoAdoptaElDeLaCompraEnLugarDePromediarConCero() {
        assertThat(recalcular("10", "0", "20", "20.60")).isEqualByComparingTo("20.60");

        // Lo que NO debe pasar: (10×0 + 20×20.60) / 30 = 13.7333
        assertThat(recalcular("10", "0", "20", "20.60")).isNotEqualByComparingTo("13.7333");
    }

    @Test
    void elGranelSePonderaConCantidadesDecimales() {
        // (25.500 × 21.84 + 40.000 × 23.50) / 65.500
        assertThat(recalcular("25.500", "21.8400", "40", "23.50")).isEqualByComparingTo("22.8537");
    }

    @Test
    void seGuardaConCuatroDecimalesPorqueElCostoSeArrastraCompraTrasCompra() {
        assertThat(recalcular("3", "10.00", "1", "11.00").scale()).isEqualTo(4);
        assertThat(recalcular("0", "0", "1", "11.00").scale()).isEqualTo(4);
    }

    @Test
    void siElCostoNoCambiaElPromedioTampoco() {
        assertThat(recalcular("10", "18.00", "20", "18.00")).isEqualByComparingTo("18.00");
    }
}
