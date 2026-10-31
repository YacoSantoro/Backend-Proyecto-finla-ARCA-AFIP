package ar.edu.utn.facturacion.dto;

import ar.edu.utn.facturacion.entity.ListaPrecioArticulo;

import java.math.BigDecimal;

/** Un renglon del explorador "Buscar ListaPrecioArticulo". */
public record PrecioArticuloDto(
        Long articuloId,
        String codigo,
        String descripcion,
        BigDecimal alicuotaIva,
        BigDecimal precio) {

    public static PrecioArticuloDto de(ListaPrecioArticulo lpa) {
        return new PrecioArticuloDto(
                lpa.getArticulo().getId(),
                lpa.getArticulo().getCodigo(),
                lpa.getArticulo().getDescripcion(),
                lpa.getArticulo().getAlicuotaIva(),
                lpa.getPrecio());
    }
}
