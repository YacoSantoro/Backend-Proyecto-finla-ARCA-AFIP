package ar.edu.utn.facturacion.web;

import ar.edu.utn.facturacion.dto.PaginaDto;
import ar.edu.utn.facturacion.dto.PrecioArticuloDto;
import ar.edu.utn.facturacion.entity.Cliente;
import ar.edu.utn.facturacion.entity.ListaPrecio;
import ar.edu.utn.facturacion.repository.ClienteRepository;
import ar.edu.utn.facturacion.repository.ListaPrecioArticuloRepository;
import ar.edu.utn.facturacion.repository.ListaPrecioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lo que usan las ventanitas "explorer" (buscar cliente / buscar articulo de la lista).
 * Son solo lecturas, por eso van directo al repositorio.
 */
@RestController
@RequestMapping("/api")
public class ExploradorController {

    private final ClienteRepository clienteRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository precioRepository;

    public ExploradorController(ClienteRepository clienteRepository, ListaPrecioRepository listaPrecioRepository,
                                ListaPrecioArticuloRepository precioRepository) {
        this.clienteRepository = clienteRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.precioRepository = precioRepository;
    }

    @GetMapping("/clientes")
    public PaginaDto<Cliente> buscarClientes(@RequestParam(defaultValue = "") String texto,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "10") int tamanio) {
        PageRequest pageable = PageRequest.of(pagina, Math.min(tamanio, 100), Sort.by("razonSocial"));
        return PaginaDto.de(clienteRepository.buscar(texto.trim(), pageable));
    }

    @GetMapping("/listas-precio")
    public List<ListaPrecio> listasDePrecio() {
        return listaPrecioRepository.findAll(Sort.by("nombre"));
    }

    @GetMapping("/listas-precio/{listaId}/articulos")
    @Transactional(readOnly = true)
    public PaginaDto<PrecioArticuloDto> buscarArticulos(@PathVariable Long listaId,
                                                        @RequestParam(defaultValue = "") String texto,
                                                        @RequestParam(defaultValue = "0") int pagina,
                                                        @RequestParam(defaultValue = "10") int tamanio) {
        PageRequest pageable = PageRequest.of(pagina, Math.min(tamanio, 100), Sort.by("articulo.codigo"));
        return PaginaDto.de(precioRepository.buscar(listaId, texto.trim(), pageable).map(PrecioArticuloDto::de));
    }
}
