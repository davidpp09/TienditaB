package tiendita.api.proveedor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByActivoTrueOrderByNombre();

    /**
     * Para no dar de alta dos veces al mismo. La colación de la base ignora
     * mayúsculas y acentos, así que "Bimbo" encuentra a "BIMBO".
     */
    Optional<Proveedor> findByNombre(String nombre);

    @Query("SELECT p FROM Proveedor p WHERE p.activo = true AND p.nombre LIKE CONCAT('%', :texto, '%') ORDER BY p.nombre")
    List<Proveedor> buscarPorNombre(@Param("texto") String texto);
}
