package ar.edu.utn.facturacion.dto;

import java.time.LocalDate;

/** Filtros de la grilla. Cualquiera puede venir en null (= no filtrar por eso). */
public record FiltroFactura(
        Long numero,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String cliente,
        Boolean informada) {
}
