package tiendita.api.producto;

/**
 * PIEZA se vende de uno en uno; KILO admite decimales (se pesa en la báscula
 * de la tienda y se teclea la cantidad). La distinción existe para que la
 * pantalla de venta sepa si debe pedir el peso.
 */
public enum Unidad {
    PIEZA,
    KILO
}
