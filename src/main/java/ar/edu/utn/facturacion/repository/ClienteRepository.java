package ar.edu.utn.facturacion.repository;

import ar.edu.utn.facturacion.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Busca por razon social o numero de documento. Con texto vacio trae todos. */
    @Query("""
            select c from Cliente c
            where lower(c.razonSocial) like lower(concat('%', :texto, '%'))
               or cast(c.docNro as string) like concat('%', :texto, '%')
            """)
    Page<Cliente> buscar(@Param("texto") String texto, Pageable pageable);
}
