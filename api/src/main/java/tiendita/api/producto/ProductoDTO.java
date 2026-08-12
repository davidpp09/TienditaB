package tiendita.api.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductoDTO {

    /** Lo que la pantalla de venta necesita saber de un producto. */
    public record Vista(
            Long id,
            String codigoBarras,
            String nombre,
            String categoria,
            Unidad unidad,
            BigDecimal precioVenta,
            BigDecimal precioMayoreo,
            BigDecimal existencia,
            BigDecimal costoPromedio,
            BigDecimal stockMinimo,
            boolean activo
    ) {
        public static Vista de(Producto p) {
            return new Vista(
                    p.getId(),
                    p.getCodigoBarras(),
                    p.getNombre(),
                    p.getCategoria() == null ? null : p.getCategoria().getNombre(),
                    p.getUnidad(),
                    p.getPrecioVenta(),
                    p.getPrecioMayoreo(),
                    p.getExistencia(),
                    p.getCostoPromedio(),
                    p.getStockMinimo(),
                    p.isActivo()
            );
        }
    }

    /**
     * Alta rápida desde el mostrador: tres campos y a seguir cobrando.
     * Todo lo demás (costo, categoría, stock mínimo) se completa después, y
     * NO puede ser obligatorio: si el alta interrumpe la venta, se abandona el
     * sistema y se vuelve al cuaderno.
     */
    public record AltaRapida(
            String codigoBarras,
            @NotBlank(message = "el nombre es obligatorio") String nombre,
            @NotNull(message = "la unidad es obligatoria") Unidad unidad,
            @NotNull(message = "el precio es obligatorio")
            @DecimalMin(value = "0.0", message = "el precio no puede ser negativo") BigDecimal precioVenta
    ) {}

    public record Edicion(
            String codigoBarras,
            @NotBlank String nombre,
            @NotNull Unidad unidad,
            @NotNull @DecimalMin("0.0") BigDecimal precioVenta,
            BigDecimal precioMayoreo,
            Long categoriaId,
            BigDecimal stockMinimo,
            Integer diasProveedor
    ) {}

    /** Merma, autoconsumo o cuadre contra conteo físico. */
    public record Ajuste(
            @NotNull BigDecimal cantidad,
            @NotBlank(message = "el motivo es obligatorio") String motivo
    ) {}
}
