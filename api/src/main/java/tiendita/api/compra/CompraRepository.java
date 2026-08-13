package tiendita.api.compra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findTop50ByOrderByFechaDescIdDesc();

    List<Compra> findByProveedorIdOrderByFechaDescIdDesc(Long proveedorId);
}
