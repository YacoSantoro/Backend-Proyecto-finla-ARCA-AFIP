package ar.edu.utn.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que manda el front para crear o modificar una factura. Los precios NO vienen: los pone el back. */
public record FacturaRequest(
        @NotNull(message = "Elegi un cliente") Long clienteId,
        @NotNull(message = "Elegi una lista de precios") Long listaPrecioId,
        @NotNull(message = "Elegi el tipo de comprobante") Integer tipoComprobante,
        LocalDate fecha,
        @NotEmpty(message = "La factura tiene que tener al menos un articulo") @Valid List<Item> items) {

    public record Item(
            @NotNull(message = "Falta el articulo") Long articuloId,
            @NotNull @Positive(message = "La cantidad tiene que ser mayor a 0") BigDecimal cantidad) {
    }
}
