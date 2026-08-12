package tiendita.api.infra;

/**
 * Error esperado y explicable al usuario del mostrador ("ese código ya existe",
 * "la venta ya estaba cancelada"). Se traduce a HTTP 400 con un mensaje en
 * español, no a un 500 con un stack trace.
 */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
