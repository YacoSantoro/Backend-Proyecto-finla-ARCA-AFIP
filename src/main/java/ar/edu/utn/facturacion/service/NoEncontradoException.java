package ar.edu.utn.facturacion.service;

/** "No existe lo que me pediste" -> HTTP 404. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
