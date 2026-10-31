package ar.edu.utn.facturacion.service;

/** "Esa factura ya fue informada a ARCA, no se toca" -> HTTP 409. */
public class FacturaBloqueadaException extends RuntimeException {

    public FacturaBloqueadaException(Long id) {
        super("La factura " + id + " ya fue informada a ARCA y no se puede modificar ni eliminar.");
    }
}
