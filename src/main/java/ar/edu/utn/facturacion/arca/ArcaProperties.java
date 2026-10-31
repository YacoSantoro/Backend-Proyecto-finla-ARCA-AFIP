package ar.edu.utn.facturacion.arca;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Todo lo que empieza con "arca." en application.properties cae aca adentro. */
@ConfigurationProperties(prefix = "arca")
public record ArcaProperties(
        boolean simulado,
        boolean produccion,
        long cuit,
        int puntoVenta,
        String clavePrivada,
        String certificado,
        String carpetaTickets) {

    public String urlWsaa() {
        return produccion
                ? "https://wsaa.afip.gov.ar/ws/services/LoginCms"
                : "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    }

    public String urlWsfe() {
        return produccion
                ? "https://servicios1.afip.gov.ar/wsfev1/service.asmx"
                : "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    }
}
