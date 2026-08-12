package tiendita.api.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByCorteIdOrderByFechaHoraAscIdAsc(Long corteId);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m WHERE m.corte.id = :corteId")
    BigDecimal sumaDelCorte(@Param("corteId") Long corteId);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m WHERE m.corte.id = :corteId AND m.tipo = :tipo")
    BigDecimal sumaDelCortePorTipo(@Param("corteId") Long corteId, @Param("tipo") TipoMovimientoCaja tipo);
}
