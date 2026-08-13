package tiendita.api.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {

    /**
     * El historial de precios de compra de un producto. Contesta "¿quién me
     * vende más barato?" y "¿desde cuándo me está subiendo este?", que son
     * preguntas que hoy se contestan de memoria.
     */
    @Query("""
            SELECT new tiendita.api.compra.CompraDTO$PrecioPagado(
                       c.fecha, c.proveedor.nombre, d.cantidad, d.costoUnitario, c.folio)
            FROM CompraDetalle d JOIN d.compra c
            WHERE d.producto.id = :productoId
            ORDER BY c.fecha DESC, c.id DESC
            """)
    List<CompraDTO.PrecioPagado> historialDe(@Param("productoId") Long productoId);

    /**
     * Lo último que se pagó por este producto. Es contra esto —y no contra el
     * costo promedio— que se compara para avisar que subió: el promedio viene
     * diluido con lo que quedaba en la bodega a precio viejo, y esa mezcla no
     * es lo que va a costar reponerlo mañana.
     */
    @Query("""
            SELECT d.costoUnitario
            FROM CompraDetalle d JOIN d.compra c
            WHERE d.producto.id = :productoId
            ORDER BY c.fecha DESC, c.id DESC
            LIMIT 1
            """)
    Optional<BigDecimal> ultimoCostoPagado(@Param("productoId") Long productoId);
}
