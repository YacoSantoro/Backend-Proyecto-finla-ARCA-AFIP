package ar.edu.utn.facturacion.web;

import ar.edu.utn.facturacion.dto.FacturaDto;
import ar.edu.utn.facturacion.dto.FacturaRequest;
import ar.edu.utn.facturacion.dto.FacturaResumenDto;
import ar.edu.utn.facturacion.dto.FiltroFactura;
import ar.edu.utn.facturacion.dto.PaginaDto;
import ar.edu.utn.facturacion.pdf.TicketPdfService;
import ar.edu.utn.facturacion.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Las "URLs" de las facturas:
 *   GET    /api/facturas              -> grilla con filtros y paginacion
 *   GET    /api/facturas/{id}         -> una factura
 *   POST   /api/facturas              -> crear
 *   PUT    /api/facturas/{id}         -> modificar (si no esta informada)
 *   DELETE /api/facturas/{id}         -> borrar   (si no esta informada)
 *   POST   /api/facturas/{id}/informar -> mandarla a ARCA
 *   GET    /api/facturas/{id}/ticket   -> ticket PDF para imprimir (solo informadas)
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final TicketPdfService ticketPdfService;

    public FacturaController(FacturaService facturaService, TicketPdfService ticketPdfService) {
        this.facturaService = facturaService;
        this.ticketPdfService = ticketPdfService;
    }

    @GetMapping
    public PaginaDto<FacturaResumenDto> buscar(
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) Boolean informada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        FiltroFactura filtro = new FiltroFactura(numero, fechaDesde, fechaHasta, cliente, informada);
        PageRequest pageable = PageRequest.of(pagina, Math.min(tamanio, 100), Sort.by(Sort.Direction.DESC, "id"));
        return facturaService.buscar(filtro, pageable);
    }

    @GetMapping("/{id}")
    public FacturaDto obtener(@PathVariable Long id) {
        return facturaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaDto crear(@Valid @RequestBody FacturaRequest request) {
        return facturaService.crear(request);
    }

    @PutMapping("/{id}")
    public FacturaDto actualizar(@PathVariable Long id, @Valid @RequestBody FacturaRequest request) {
        return facturaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
    }

    @PostMapping("/{id}/informar")
    public FacturaDto informar(@PathVariable Long id) {
        return facturaService.informar(id);
    }

    /** Devuelve el PDF "inline": el navegador lo abre en una pestaña y desde ahi se imprime. */
    @GetMapping("/{id}/ticket")
    public ResponseEntity<byte[]> ticket(@PathVariable Long id) {
        byte[] pdf = ticketPdfService.generar(id);
        String nombre = ticketPdfService.nombreArchivo(id) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nombre).build().toString())
                .body(pdf);
    }
}
