package tiendita.api.venta;

import java.math.BigDecimal;

/**
 * Lo vendido y lo ganado en un periodo. La distinción es el punto entero del
 * sistema: {@code total} es lo que entró, {@code utilidad} es lo que se ganó.
 */
public record ResumenVentas(BigDecimal total, BigDecimal costo, Long tickets) {

    public BigDecimal utilidad() {
        return total.subtract(costo);
    }
}
