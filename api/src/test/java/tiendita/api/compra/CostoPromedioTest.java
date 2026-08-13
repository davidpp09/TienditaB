package tiendita.api.compra;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

    // ---- Devolver al proveedor: deshacer la ponderación ---------------------

    private BigDecimal revertir(String existencia, String costoActual, String cantidad, String costoDevuelto) {
        return CostoPromedio.revertir(new BigDecimal(existencia), new BigDecimal(costoActual),
                new BigDecimal(cantidad), new BigDecimal(costoDevuelto));
    }

    /**
     * La prueba que justifica toda la fórmula, y la que dice si devolver está
     * bien hecho: se tenían 10 piezas a $18.00, entraron 20 caras a $20.60
     * dejando el promedio en $19.7333, y se devuelve el lote caro completo. El
     * promedio tiene que volver <b>exactamente</b> a $18.00.
     *
     * <p>Sacando las piezas al costo promedio —que es lo que hace una merma, y
     * lo más fácil de escribir— el promedio se quedaría en $19.7333 para
     * siempre: contaminado con un costo que ya se deshizo.
     *
     * <p>Vuelve a $17.9999 y no a $18.0000 exacto, y eso está bien: el promedio
     * se guarda con 4 decimales, así que $19.73333… se guardó como $19.7333 y
     * esa diezmilésima perdida ya no se puede recuperar. Es una diezmilésima de
     * peso por pieza; para que se notara un centavo harían falta cien piezas y
     * el mismo ciclo de comprar y devolver repetido. Se anota aquí para que
     * quede claro que es el redondeo y no un error de la fórmula — y si algún
     * día importara, la verdad se reconstruye recorriendo el kardex.
     */
    @Test
    void devolverElLoteCompletoDejaElPromedioComoEstabaAntes() {
        BigDecimal despuesDeComprar = recalcular("10", "18.00", "20", "20.60");
        assertThat(despuesDeComprar).isEqualByComparingTo("19.7333");

        BigDecimal despuesDeDevolver = revertir("30", despuesDeComprar.toPlainString(), "20", "20.60");

        assertThat(despuesDeDevolver).isEqualByComparingTo("17.9999");
        assertThat(despuesDeDevolver).isCloseTo(new BigDecimal("18.00"), within(new BigDecimal("0.0001")));
    }

    @Test
    void devolverLaMitadDejaElPromedioEnMedio() {
        // (30 × 19.7333 − 10 × 20.60) / 20
        assertThat(revertir("30", "19.7333", "10", "20.60")).isEqualByComparingTo("19.3000");
    }

    /** Si no queda nada, no hay nada que promediar: el costo se deja quieto. */
    @Test
    void devolverTodaLaExistenciaDejaElCostoComoEstaba() {
        assertThat(revertir("20", "20.60", "20", "20.60")).isEqualByComparingTo("20.60");
    }

    /**
     * Solo pasa con datos ya torcidos, pero un costo negativo contaminaría cada
     * venta que se hiciera después. Dejarlo quieto es lo menos malo.
     */
    @Test
    void nuncaDejaUnCostoNegativo() {
        assertThat(revertir("10", "5.00", "2", "100.00")).isEqualByComparingTo("5.00");
    }

    @Test
    void devolverGranelSeReviertConDecimales() {
        // (12.500 × 24.80 − 2.500 × 24.80) / 10.000 = 24.80
        assertThat(revertir("12.500", "24.8000", "2.500", "24.80")).isEqualByComparingTo("24.80");
    }
}
