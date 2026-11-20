package ar.edu.utn.facturacion.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos del comercio que se imprimen en el encabezado del ticket ("emisor." en application.properties). */
@ConfigurationProperties(prefix = "emisor")
public record EmisorProperties(
        String razonSocial,
        String domicilio,
        String condicionIva,
        String ingresosBrutos,
        String inicioActividades) {
}
