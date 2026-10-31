package ar.edu.utn.facturacion.service;

/** "Lo que pediste rompe una regla del negocio" -> HTTP 422. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
