package tiendita.api.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CorteCajaRepository extends JpaRepository<CorteCaja, Long> {

    /** El corte que sigue abierto. A lo sumo hay uno. */
    Optional<CorteCaja> findFirstByCerradoEnIsNullOrderByAbiertoEnDesc();

    List<CorteCaja> findTop30ByCerradoEnIsNotNullOrderByFechaDescIdDesc();
}
