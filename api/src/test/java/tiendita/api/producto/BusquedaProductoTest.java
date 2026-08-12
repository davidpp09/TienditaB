package tiendita.api.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.infra.ReglaDeNegocioException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusquedaProductoTest {

    @Autowired ProductoService servicio;
    @Autowired ProductoRepository productos;

    /**
     * Nadie va a teclear el acento en el mostrador. La colación de la base
     * (utf8mb4_unicode_ci) ignora acentos y mayúsculas, así que la búsqueda
     * funciona sin normalizar nada en Java. Este test fija ese supuesto: si
     * algún día cambia la colación, salta aquí y no en la caja.
     */
    @Test
    void buscarSinAcentosEncuentraLoAcentuado() {
        productos.save(new Producto(null, "Atún en agua", Unidad.PIEZA, new BigDecimal("21.00")));

        assertThat(servicio.buscar("atun")).extracting(Producto::getNombre).contains("Atún en agua");
        assertThat(servicio.buscar("ATUN")).extracting(Producto::getNombre).contains("Atún en agua");
    }

    @Test
    void elAltaRapidaNecesitaSoloTresCampos() {
        Producto nuevo = servicio.altaRapida(new ProductoDTO.AltaRapida(
                "7501055300006", "Sabritas adobadas", Unidad.PIEZA, new BigDecimal("19.50")));

        assertThat(nuevo.getId()).isNotNull();
        assertThat(nuevo.getExistencia()).isEqualByComparingTo("0");
        assertThat(nuevo.getCostoPromedio()).isEqualByComparingTo("0");
        assertThat(servicio.porCodigo("7501055300006")).isPresent();
    }

    @Test
    void noSePuedenRepetirLosCodigosDeBarras() {
        servicio.altaRapida(new ProductoDTO.AltaRapida("7501055300006", "Sabritas", Unidad.PIEZA, new BigDecimal("19.50")));

        assertThatThrownBy(() -> servicio.altaRapida(new ProductoDTO.AltaRapida(
                "7501055300006", "Otra cosa", Unidad.PIEZA, new BigDecimal("10.00"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    /** El granel no tiene código, y "" no es un código: tiene que quedar NULL. */
    @Test
    void variosProductosPuedenNoTenerCodigo() {
        servicio.altaRapida(new ProductoDTO.AltaRapida("", "Frijol a granel", Unidad.KILO, new BigDecimal("32.00")));
        Producto arroz = servicio.altaRapida(
                new ProductoDTO.AltaRapida(null, "Arroz a granel", Unidad.KILO, new BigDecimal("28.00")));

        productos.flush();
        assertThat(arroz.getCodigoBarras()).isNull();
    }
}
