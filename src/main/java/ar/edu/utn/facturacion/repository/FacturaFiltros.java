package ar.edu.utn.facturacion.repository;

import ar.edu.utn.facturacion.dto.FiltroFactura;
import ar.edu.utn.facturacion.entity.Factura;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte los filtros de la pantalla en un WHERE.
 * Cada filtro que viene vacio simplemente no se agrega.
 */
public final class FacturaFiltros {

    private FacturaFiltros() {
    }

    public static Specification<Factura> desde(FiltroFactura filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (filtro.numero() != null) {
                condiciones.add(cb.equal(root.get("numero"), filtro.numero()));
            }
            if (filtro.fechaDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), filtro.fechaDesde()));
            }
            if (filtro.fechaHasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), filtro.fechaHasta()));
            }
            if (filtro.cliente() != null && !filtro.cliente().isBlank()) {
                String patron = "%" + filtro.cliente().trim().toLowerCase() + "%";
                condiciones.add(cb.like(cb.lower(root.get("cliente").get("razonSocial")), patron));
            }
            if (filtro.informada() != null) {
                condiciones.add(cb.equal(root.get("informadaArca"), filtro.informada()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
