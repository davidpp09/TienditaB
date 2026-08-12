package tiendita.api.venta;

public enum FormaPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA;

    /** Solo el efectivo mueve el cajón, y por tanto el corte de caja. */
    public boolean mueveElCajon() {
        return this == EFECTIVO;
    }
}
