package ar.edu.utn.facturacion.repository;

import ar.edu.utn.facturacion.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** JpaSpecificationExecutor nos deja armar el WHERE "a pedido" segun los filtros que vengan. */
public interface FacturaRepository extends JpaRepository<Factura, Long>, JpaSpecificationExecutor<Factura> {

    /** Igual que el findAll de siempre, pero trae el cliente en la misma consulta (para la grilla). */
    @Override
    @EntityGraph(attributePaths = "cliente")
    Page<Factura> findAll(Specification<Factura> spec, Pageable pageable);

    @Query("select max(f.numero) from Factura f where f.puntoVenta = :pv and f.tipoComprobante = :tipo")
    Optional<Long> ultimoNumero(@Param("pv") int puntoVenta, @Param("tipo") int tipoComprobante);
}
