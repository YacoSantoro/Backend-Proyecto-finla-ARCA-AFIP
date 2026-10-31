package ar.edu.utn.facturacion.service;

/** Codigos de ARCA para los comprobantes que manejamos. */
public final class TipoComprobante {

    public static final int FACTURA_A = 1;
    public static final int FACTURA_B = 6;
    public static final int FACTURA_C = 11;

    private TipoComprobante() {
    }

    public static boolean esValido(int tipo) {
        return tipo == FACTURA_A || tipo == FACTURA_B || tipo == FACTURA_C;
    }

    /** La factura C (monotributo) no discrimina IVA. */
    public static boolean discriminaIva(int tipo) {
        return tipo != FACTURA_C;
    }
}
