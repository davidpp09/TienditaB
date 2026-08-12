package tiendita.api.kardex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoIdOrderByFechaHoraDescIdDesc(Long productoId);

    /**
     * Suma de todos los movimientos de un producto. Debe dar exactamente su
     * existencia: si no, el caché se desincronizó del libro. Ver KardexCuadraTest.
     */
    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoInventario m WHERE m.producto.id = :productoId")
    BigDecimal sumaCantidades(@Param("productoId") Long productoId);
}
