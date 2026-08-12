package tiendita.api.caja;

/**
 * En esta tabla solo entra el dinero que entra o sale DEL CAJÓN. Una venta con
 * tarjeta no mueve el cajón, así que no genera movimiento: si contara, el corte
 * pediría un efectivo que nunca estuvo ahí. El total vendido del día sale de la
 * tabla `venta`, no de aquí.
 */
public enum TipoMovimientoCaja {
    /** El dinero que se deja para dar cambio al abrir. */
    FONDO,
    /** Venta cobrada en efectivo. Una cancelación entra como VENTA en negativo. */
    VENTA,
    /** Luz, renta, gasolina, sueldos. */
    GASTO,
    /** Pago a proveedor en efectivo (Fase 2). */
    COMPRA,
    /** Dinero que se lleva el dueño. */
    RETIRO,
    /** Dinero que se mete al cajón fuera de una venta. */
    DEPOSITO
}
