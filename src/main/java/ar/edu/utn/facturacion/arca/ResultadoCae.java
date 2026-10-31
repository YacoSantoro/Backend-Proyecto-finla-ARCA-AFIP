package ar.edu.utn.facturacion.arca;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que contesto ARCA.
 * resultado: "A" = Aprobada (tiene CAE), "R" = Rechazada (mirar observaciones / errores).
 */
public record ResultadoCae(
        long numero,
        String resultado,
        String cae,
        LocalDate caeVencimiento,
        List<String> observaciones,
        List<String> errores) {

    public boolean aprobado() {
        return "A".equals(resultado) && cae != null && !cae.isBlank();
    }

    /** Observaciones + errores en un solo texto, para guardar y mostrar. */
    public String mensajes() {
        return String.join(" | ", java.util.stream.Stream.concat(errores.stream(), observaciones.stream()).toList());
    }
}
