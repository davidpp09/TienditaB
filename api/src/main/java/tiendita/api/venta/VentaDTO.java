package tiendita.api.venta;

import tiendita.api.comun.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VentaDTO {

    /**
     * Lo que manda la pantalla de venta al cobrar. Ojo con lo que NO viene aquí:
     * el precio. El cliente manda qué y cuánto; el precio lo pone el servidor,
     * leyéndolo del catálogo. Si el precio viajara desde el navegador, cualquiera
     * con la consola abierta podría cobrarse a sí mismo a $1.
     */
    public record Cobro(
            @NotEmpty(message = "la venta no tiene renglones") @Valid List<Linea> lineas,
            @NotNull(message = "falta la forma de pago") FormaPago formaPago,
            BigDecimal recibido
    ) {}

    public record Linea(
            @NotNull(message = "falta el producto") Long productoId,
            @NotNull(message = "falta la cantidad") BigDecimal cantidad
    ) {}

    public record LineaVista(
            Long productoId,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal importe
    ) {
        static LineaVista de(VentaDetalle d) {
            return new LineaVista(d.getProducto().getId(), d.getDescripcion(), d.getCantidad(),
                    d.getPrecioUnitario(), d.getImporte());
        }
    }

    public record Vista(
            Long id,
            String folio,
            LocalDateTime fechaHora,
            BigDecimal total,
            BigDecimal recibido,
            BigDecimal cambio,
            FormaPago formaPago,
            boolean cancelada,
            List<LineaVista> lineas
    ) {
        public static Vista de(Venta v) {
            return new Vista(v.getId(), v.getFolio(), v.getFechaHora(), v.getTotal(), v.getRecibido(),
                    v.getCambio(), v.getFormaPago(), v.isCancelada(),
                    v.getDetalles().stream().map(LineaVista::de).toList());
        }
    }

    public record Cancelacion(String motivo) {}
}
