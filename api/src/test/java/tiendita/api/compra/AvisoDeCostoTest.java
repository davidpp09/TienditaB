package tiendita.api.compra;

import org.junit.jupiter.api.Test;
import tiendita.api.producto.Producto;
import tiendita.api.producto.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/** Aritmética pura otra vez: el aviso no necesita base de datos para probarse. */
class AvisoDeCostoTest {

    private Producto producto(String nombre, String precio) {
        return new Producto(null, nombre, Unidad.PIEZA, new BigDecimal(precio));
    }

    private AvisoDeCosto aviso(String precio, String costoAnterior, String costoNuevo) {
        return AvisoDeCosto.siSubio(producto("Atún en agua", precio),
                costoAnterior == null ? null : new BigDecimal(costoAnterior),
                new BigDecimal(costoNuevo));
    }

    /** El caso del handbook: el atún subió y el precio se quedó donde estaba. */
    @Test
    void avisaCuantoSubioYQueMargenQueda() {
        AvisoDeCosto a = aviso("21.00", "18.50", "20.60");

        assertThat(a.subioPorciento()).isEqualByComparingTo("11.4");    // 2.10 sobre 18.50
        assertThat(a.margenAnterior()).isEqualByComparingTo("11.9");    // 2.50 sobre 21.00
        assertThat(a.margenNuevo()).isEqualByComparingTo("1.9");        // 0.40 sobre 21.00
        assertThat(a.precioSugerido()).isEqualByComparingTo("23.38");
        assertThat(a.mensaje()).isEqualTo(
                "Atún en agua pasó de $18.50 a $20.60 (+11.4%). A tu precio de $21.00 te queda "
                        + "1.9% de margen, de 11.9% que tenías. Para conservarlo: $23.38.");
    }

    /**
     * La prueba que de verdad importa del precio sugerido: que al ponerlo,
     * el margen vuelva a ser el de antes. Si esta cuenta está al revés
     * (margen sobre el costo en lugar de sobre el precio), el número se ve
     * razonable y aun así deja el precio corto.
     */
    @Test
    void elPrecioSugeridoDevuelveElMargenQueSeTenia() {
        AvisoDeCosto a = aviso("21.00", "18.50", "20.60");

        BigDecimal margenAlPrecioSugerido = a.precioSugerido().subtract(a.costoNuevo())
                .multiply(BigDecimal.valueOf(100))
                .divide(a.precioSugerido(), 1, RoundingMode.HALF_UP);

        assertThat(margenAlPrecioSugerido).isEqualByComparingTo(a.margenAnterior());
    }

    @Test
    void noAvisaCuandoElCostoBajaOSeQuedaIgual() {
        assertThat(aviso("21.00", "20.60", "18.50")).isNull();
        assertThat(aviso("21.00", "18.50", "18.50")).isNull();
    }

    /**
     * Primera compra del producto: no hay contra qué comparar. Avisar aquí
     * llenaría de ruido el alta de cada producto nuevo y entrenaría a David
     * para ignorar los avisos, que es la única manera de que esta función deje
     * de servir.
     */
    @Test
    void noAvisaEnLaPrimeraCompraDeUnProducto() {
        assertThat(aviso("21.00", null, "20.60")).isNull();
        assertThat(aviso("21.00", "0", "20.60")).isNull();
    }

    /** El caso que hay que ver a tiempo: ya se está vendiendo por debajo del costo. */
    @Test
    void avisaCuandoElProductoYaSeVendeAPerdida() {
        AvisoDeCosto a = aviso("21.00", "18.50", "22.00");

        assertThat(a.margenNuevo()).isNegative();
        assertThat(a.mensaje()).contains("lo estás vendiendo a pérdida");
        assertThat(a.precioSugerido()).isEqualByComparingTo("24.97");   // 22.00 / (1 − 0.119)
    }

    /** Un producto sin precio de venta no tiene margen que calcular. */
    @Test
    void elProductoSinPrecioAvisaPeroNoInventaUnMargen() {
        AvisoDeCosto a = AvisoDeCosto.siSubio(producto("Bolsas", "0.00"),
                new BigDecimal("10.00"), new BigDecimal("12.00"));

        assertThat(a).isNotNull();
        assertThat(a.margenNuevo()).isNull();
        assertThat(a.mensaje()).contains("No tiene precio de venta puesto");
    }
}
