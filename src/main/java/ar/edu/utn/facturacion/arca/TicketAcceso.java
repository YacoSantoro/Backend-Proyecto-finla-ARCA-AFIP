package ar.edu.utn.facturacion.arca;

import java.time.Duration;
import java.time.Instant;

/** El "pase" que da WSAA: con token + sign podes usar WSFE hasta la expiracion (~12 hs). */
public record TicketAcceso(String token, String sign, Instant expiracion) {

    /** Lo damos por vencido 10 minutos antes, por las dudas. */
    public boolean vigente() {
        return Instant.now().isBefore(expiracion.minus(Duration.ofMinutes(10)));
    }
}
