package ar.edu.utn.facturacion.dto;

import ar.edu.utn.facturacion.entity.Factura;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila de la grilla de facturas. */
public record FacturaResumenDto(
        Long id,
        int puntoVenta,
        int tipoComprobante,
        Long numero,
        LocalDate fecha,
        String cliente,
        BigDecimal importeTotal,
        boolean informadaArca,
        String cae) {

    public static FacturaResumenDto de(Factura f) {
        return new FacturaResumenDto(f.getId(), f.getPuntoVenta(), f.getTipoComprobante(), f.getNumero(),
                f.getFecha(), f.getCliente().getRazonSocial(), f.getImporteTotal(), f.isInformadaArca(), f.getCae());
    }
}
