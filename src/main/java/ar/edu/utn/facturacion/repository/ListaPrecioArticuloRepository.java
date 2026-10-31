package ar.edu.utn.facturacion.repository;

import ar.edu.utn.facturacion.entity.ListaPrecioArticulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    /** Articulos de una lista, filtrando por codigo o descripcion. */
    @Query(value = """
            select lpa from ListaPrecioArticulo lpa join fetch lpa.articulo a
            where lpa.listaPrecio.id = :listaId
              and (lower(a.codigo) like lower(concat('%', :texto, '%'))
                   or lower(a.descripcion) like lower(concat('%', :texto, '%')))
            """,
            countQuery = """
            select count(lpa) from ListaPrecioArticulo lpa join lpa.articulo a
            where lpa.listaPrecio.id = :listaId
              and (lower(a.codigo) like lower(concat('%', :texto, '%'))
                   or lower(a.descripcion) like lower(concat('%', :texto, '%')))
            """)
    Page<ListaPrecioArticulo> buscar(@Param("listaId") Long listaId, @Param("texto") String texto, Pageable pageable);

    @Query("""
            select lpa from ListaPrecioArticulo lpa join fetch lpa.articulo
            where lpa.listaPrecio.id = :listaId and lpa.articulo.id = :articuloId
            """)
    Optional<ListaPrecioArticulo> buscarPrecio(@Param("listaId") Long listaId, @Param("articuloId") Long articuloId);
}
