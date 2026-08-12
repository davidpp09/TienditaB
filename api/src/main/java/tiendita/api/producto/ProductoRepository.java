package tiendita.api.producto;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    /**
     * Búsqueda del mostrador. La colación de la base es utf8mb4_unicode_ci, que
     * ignora mayúsculas Y acentos: `higado` encuentra "Hígado". Por eso aquí no
     * hace falta normalizar en Java — lo hace la base. Lo fija BusquedaProductoTest.
     */
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.nombre LIKE CONCAT('%', :texto, '%') ORDER BY p.nombre")
    List<Producto> buscarPorNombre(@Param("texto") String texto);

    List<Producto> findByActivoTrueOrderByNombre();

    /**
     * Bloquea el renglón mientras se mueve la existencia. Con una sola caja
     * registradora la carrera es improbable, pero David consulta y ajusta desde
     * el celular: dos escrituras simultáneas sobre el mismo producto perderían
     * una. Son cuatro líneas que eliminan una clase entera de error.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdConBloqueo(@Param("id") Long id);

    /** Para la alerta de stock bajo del correo diario. */
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stockMinimo > 0 AND p.existencia <= p.stockMinimo ORDER BY p.nombre")
    List<Producto> conStockBajo();
}
