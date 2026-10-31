package ar.edu.utn.facturacion.arca;

import ar.edu.utn.facturacion.service.CalculadoraImportes.Totales;

import java.time.LocalDate;

/** Todos los datos que ARCA necesita para autorizar UNA factura. */
public record SolicitudCae(
        int puntoVenta,
        int tipoComprobante,
        long numero,
        LocalDate fecha,
        int docTipo,
        long docNro,
        int condicionIvaReceptor,
        Totales totales) {
}
