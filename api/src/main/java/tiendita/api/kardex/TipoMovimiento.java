package tiendita.api.kardex;

public enum TipoMovimiento {
    /** Entrada por compra a proveedor (Fase 2). */
    COMPRA,
    /** Salida por venta en el mostrador. */
    VENTA,
    /** Salida por rotura, caducidad o robo. Exige motivo. */
    MERMA,
    /** Salida de lo que se llevó la casa. Aparte de la merma a propósito. */
    AUTOCONSUMO,
    /** Cuadre contra un conteo físico. Exige motivo. */
    AJUSTE,
    /** Salida por devolución a proveedor. */
    DEVOLUCION_PROVEEDOR,
    /** Entrada que revierte una venta cancelada. */
    CANCELACION
}
