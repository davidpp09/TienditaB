package tiendita.api.proveedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ProveedorDTO {

    public record Vista(
            Long id,
            String nombre,
            String telefono,
            int diasEntrega,
            String notas,
            boolean activo
    ) {
        public static Vista de(Proveedor p) {
            return new Vista(p.getId(), p.getNombre(), p.getTelefono(), p.getDiasEntrega(),
                    p.getNotas(), p.isActivo());
        }
    }

    /**
     * Solo el nombre es obligatorio. Igual que en el alta rápida de productos:
     * si dar de alta al proveedor obliga a buscar su teléfono, la compra se
     * queda sin capturar y el costo nunca se actualiza.
     */
    public record Alta(
            @NotBlank(message = "el nombre es obligatorio") String nombre,
            String telefono,
            @Positive(message = "los días de entrega deben ser mayores que cero") Integer diasEntrega,
            String notas
    ) {}
}
