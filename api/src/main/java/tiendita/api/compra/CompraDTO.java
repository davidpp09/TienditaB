package tiendita.api.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tiendita.api.comun.FormaPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CompraDTO {

    /**
     * Lo que se captura cuando llega el proveedor.
     *
     * <p>Al revés que en la venta, aquí el costo SÍ viaja desde el cliente: es
     * el dato nuevo, el que trae la factura en la mano y que el sistema no
     * puede saber de ningún otro lado. Lo que no viaja es el total — ese lo
     * suma el servidor, para que no pueda haber una compra cuyo total no
     * cuadre con sus renglones.
     */
    public record Registro(
            @NotNull(message = "falta el proveedor") Long proveedorId,
            LocalDate fecha,
            String folio,
            Boolean pagada,
            FormaPago formaPago,
            @NotEmpty(message = "la compra no tiene renglones") @Valid List<Linea> lineas
    ) {}

    public record Linea(
            @NotNull(message = "falta el producto") Long productoId,
            @NotNull(message = "falta la cantidad") BigDecimal cantidad,
            @NotNull(message = "falta el costo")
            @DecimalMin(value = "0.0", message = "el costo no puede ser negativo") BigDecimal costoUnitario
    ) {}

    public record LineaVista(
            Long productoId,
            String producto,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal importe,
            /** El costo promedio con el que quedó el producto después de esta compra. */
            BigDecimal costoPromedioResultante,
            BigDecimal existenciaResultante
    ) {
        static LineaVista de(CompraDetalle d) {
            return new LineaVista(d.getProducto().getId(), d.getProducto().getNombre(), d.getCantidad(),
                    d.getCostoUnitario(), d.getImporte(),
                    d.getProducto().getCostoPromedio(), d.getProducto().getExistencia());
        }
    }

    public record Vista(
            Long id,
            Long proveedorId,
            String proveedor,
            LocalDate fecha,
            String folio,
            BigDecimal total,
            boolean pagada,
            FormaPago formaPago,
            boolean salioDelCajon,
            List<LineaVista> lineas,
            /** Los productos que subieron de costo con esta compra. Vacío casi siempre. */
            List<AvisoDeCosto> avisos
    ) {
        public static Vista de(Compra c, List<AvisoDeCosto> avisos) {
            return new Vista(c.getId(), c.getProveedor().getId(), c.getProveedor().getNombre(),
                    c.getFecha(), c.getFolio(), c.getTotal(), c.isPagada(), c.getFormaPago(),
                    c.salioDelCajon(),
                    c.getDetalles().stream().map(LineaVista::de).toList(),
                    avisos);
        }
    }

    /**
     * Lo que se le regresa al proveedor: llegó caduco, roto, o de más.
     *
     * <p>Aquí no viaja el costo, al revés que en la compra. El costo sale del
     * renglón de la compra original, que es lo que de verdad se pagó por esas
     * piezas. Si viajara desde el cliente, se podría "devolver" a un costo
     * inventado y quedarse con la diferencia.
     */
    public record Devolucion(
            @NotBlank(message = "el motivo es obligatorio") String motivo,
            @NotEmpty(message = "no hay nada que devolver") @Valid List<LineaDevuelta> lineas
    ) {}

    public record LineaDevuelta(
            @NotNull(message = "falta el producto") Long productoId,
            @NotNull(message = "falta la cantidad") BigDecimal cantidad
    ) {}

    public record LineaDevueltaVista(
            Long productoId,
            String producto,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal importe,
            BigDecimal existenciaResultante,
            BigDecimal costoPromedioResultante
    ) {}

    public record DevolucionVista(
            Long compraId,
            String proveedor,
            String folio,
            String motivo,
            /** El dinero que el proveedor tiene que regresar. */
            BigDecimal total,
            /** Verdadero solo si esa compra había salido del cajón en efectivo. */
            boolean regresoAlCajon,
            List<LineaDevueltaVista> lineas
    ) {}

    /** Un renglón del historial: qué se pagó por este producto, cuándo y a quién. */
    public record PrecioPagado(
            LocalDate fecha,
            String proveedor,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String folio
    ) {}
}
