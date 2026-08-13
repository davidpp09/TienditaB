package tiendita.api.proveedor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.infra.ReglaDeNegocioException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProveedorTest {

    @Autowired ProveedorService proveedores;

    @Test
    void bastaElNombreParaDarDeAltaUnProveedor() {
        Proveedor bimbo = proveedores.alta(new ProveedorDTO.Alta("Bimbo", null, null, null));

        assertThat(bimbo.getId()).isNotNull();
        assertThat(bimbo.getNombre()).isEqualTo("Bimbo");
        assertThat(bimbo.getDiasEntrega()).isEqualTo(7);   // el que trae por omisión
        assertThat(bimbo.isActivo()).isTrue();
    }

    @Test
    void noSeDaDeAltaDosVecesElMismoProveedor() {
        proveedores.alta(new ProveedorDTO.Alta("Coca-Cola", "3312345678", 3, null));

        assertThatThrownBy(() -> proveedores.alta(new ProveedorDTO.Alta("Coca-Cola", null, null, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe el proveedor");
    }

    /**
     * La colación de la base (utf8mb4_unicode_ci) ignora mayúsculas y acentos,
     * así que el duplicado se detecta aunque se escriba distinto. Es la misma
     * propiedad de la que depende el buscador de productos del mostrador.
     */
    @Test
    void elDuplicadoSeDetectaAunqueCambienMayusculasYAcentos() {
        proveedores.alta(new ProveedorDTO.Alta("Lácteos del Valle", null, null, null));

        assertThatThrownBy(() -> proveedores.alta(new ProveedorDTO.Alta("LACTEOS DEL VALLE", null, null, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe el proveedor");
    }

    @Test
    void elNombreSeGuardaSinEspaciosDeSobra() {
        Proveedor sabritas = proveedores.alta(new ProveedorDTO.Alta("  Sabritas  ", null, null, null));

        assertThat(sabritas.getNombre()).isEqualTo("Sabritas");
    }

    @Test
    void editarNoChocaConsigoMismo() {
        Proveedor marinela = proveedores.alta(new ProveedorDTO.Alta("Marinela", null, null, null));

        Proveedor editado = proveedores.editar(marinela.getId(),
                new ProveedorDTO.Alta("Marinela", "3399887766", 5, "pasa los martes"));

        assertThat(editado.getTelefono()).isEqualTo("3399887766");
        assertThat(editado.getDiasEntrega()).isEqualTo(5);
        assertThat(editado.getNotas()).isEqualTo("pasa los martes");
    }

    @Test
    void elProveedorDadoDeBajaDesapareceDeLaBusquedaPeroNoDeLaBase() {
        Proveedor viejo = proveedores.alta(new ProveedorDTO.Alta("Refrescos del Norte", null, null, null));

        proveedores.desactivar(viejo.getId());

        assertThat(proveedores.buscar(null)).noneMatch(p -> p.getId().equals(viejo.getId()));
        assertThat(proveedores.porId(viejo.getId()).isActivo()).isFalse();
    }
}
