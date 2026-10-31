package ar.edu.utn.facturacion.arca;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * El "cartero": manda un sobre SOAP (un XML) por HTTP POST y devuelve el XML de respuesta.
 * SOAP no es magia: es un POST con un XML adentro.
 */
@Component
public class SoapHttp {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String enviar(String url, String soapAction, String xml) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", soapAction)
                .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                .build();
        try {
            // Aunque ARCA conteste 500 (SOAP Fault) devolvemos el cuerpo: ahi viene el mensaje de error
            return http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (IOException e) {
            throw new ArcaException("No se pudo conectar con ARCA (" + url + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArcaException("Se interrumpio la conexion con ARCA", e);
        }
    }
}
