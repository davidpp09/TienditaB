package tiendita.api.venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findTop50ByOrderByIdDesc();

    List<Venta> findByFechaHoraBetweenAndCanceladaFalseOrderByIdDesc(LocalDateTime desde, LocalDateTime hasta);

    /**
     * Venta y costo del periodo, en una sola pasada. Las canceladas quedan fuera:
     * siguen en la tabla, pero no son venta.
     */
    @Query("""
            SELECT new tiendita.api.venta.ResumenVentas(
                       COALESCE(SUM(v.total), 0), COALESCE(SUM(v.costoTotal), 0), COUNT(v))
            FROM Venta v
            WHERE v.cancelada = false AND v.fechaHora BETWEEN :desde AND :hasta
            """)
    ResumenVentas resumen(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
