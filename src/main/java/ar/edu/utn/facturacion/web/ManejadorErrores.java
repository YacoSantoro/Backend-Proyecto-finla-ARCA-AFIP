package ar.edu.utn.facturacion.web;

import ar.edu.utn.facturacion.arca.ArcaException;
import ar.edu.utn.facturacion.service.FacturaBloqueadaException;
import ar.edu.utn.facturacion.service.NegocioException;
import ar.edu.utn.facturacion.service.NoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Atrapa las excepciones y las convierte en una respuesta JSON prolija:
 *   { "mensaje": "...", "detalles": ["..."] }
 * Asi el front siempre sabe donde buscar el texto del error.
 */
@RestControllerAdvice
public class ManejadorErrores {

    public record Error(String mensaje, List<String> detalles) {
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Error> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(FacturaBloqueadaException.class)
    public ResponseEntity<Error> bloqueada(FacturaBloqueadaException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Error> negocio(NegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), List.of());
    }

    @ExceptionHandler(ArcaException.class)
    public ResponseEntity<Error> arca(ArcaException e) {
        return responder(HttpStatus.BAD_GATEWAY, "Problema con ARCA: " + e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Hay datos invalidos", detalles);
    }

    private ResponseEntity<Error> responder(HttpStatus status, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(new Error(mensaje, detalles));
    }
}
