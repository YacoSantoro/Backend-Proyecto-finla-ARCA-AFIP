package ar.edu.utn.facturacion.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Lo que recibe el front para dibujar una grilla paginada. */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaDto<T> de(Page<T> page) {
        return new PaginaDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
