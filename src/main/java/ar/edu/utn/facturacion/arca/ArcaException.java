package ar.edu.utn.facturacion.arca;

/** Fallo hablando con ARCA (sin internet, certificado mal, ARCA caido...) -> HTTP 502. */
public class ArcaException extends RuntimeException {

    public ArcaException(String mensaje) {
        super(mensaje);
    }

    public ArcaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
