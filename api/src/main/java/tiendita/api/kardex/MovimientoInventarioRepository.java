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

    /**
     * Lo que ya se le devolvió al proveedor de un producto de una compra, en
     * positivo. Sin esto se podría devolver el mismo lote dos veces y cobrarlo
     * dos veces, y el kardex terminaría sacando más piezas de las que entraron.
     * <p>
     * Sale del kardex y no de una tabla de devoluciones porque el kardex ya es
     * el libro: la devolución vive ahí, con su referencia a la compra y su
     * motivo, y una tabla aparte solo sería una segunda versión de la verdad.
     */
    @Query("""
            SELECT COALESCE(SUM(-m.cantidad), 0)
            FROM MovimientoInventario m
            WHERE m.producto.id = :productoId
              AND m.tipo = tiendita.api.kardex.TipoMovimiento.DEVOLUCION_PROVEEDOR
              AND m.referenciaTipo = 'COMPRA'
              AND m.referenciaId = :compraId
            """)
    BigDecimal yaDevueltoDeLaCompra(@Param("productoId") Long productoId, @Param("compraId") Long compraId);
}
