package ar.edu.utn.facturacion.service;

import ar.edu.utn.facturacion.arca.ArcaProperties;
import ar.edu.utn.facturacion.arca.ArcaService;
import ar.edu.utn.facturacion.arca.ResultadoCae;
import ar.edu.utn.facturacion.dto.FacturaDto;
import ar.edu.utn.facturacion.dto.FacturaRequest;
import ar.edu.utn.facturacion.dto.FacturaResumenDto;
import ar.edu.utn.facturacion.dto.FiltroFactura;
import ar.edu.utn.facturacion.dto.PaginaDto;
import ar.edu.utn.facturacion.entity.Cliente;
import ar.edu.utn.facturacion.entity.Factura;
import ar.edu.utn.facturacion.entity.FacturaDetalle;
import ar.edu.utn.facturacion.entity.ListaPrecio;
import ar.edu.utn.facturacion.entity.ListaPrecioArticulo;
import ar.edu.utn.facturacion.repository.ClienteRepository;
import ar.edu.utn.facturacion.repository.FacturaFiltros;
import ar.edu.utn.facturacion.repository.FacturaRepository;
import ar.edu.utn.facturacion.repository.ListaPrecioArticuloRepository;
import ar.edu.utn.facturacion.repository.ListaPrecioRepository;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Linea;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Totales;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Aca viven las reglas de la factura. El controller solo recibe y devuelve; el que piensa es este. */
@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository precioRepository;
    private final ArcaService arcaService;
    private final ArcaProperties arcaProperties;

    public FacturaService(FacturaRepository facturaRepository, ClienteRepository clienteRepository,
                          ListaPrecioRepository listaPrecioRepository, ListaPrecioArticuloRepository precioRepository,
                          ArcaService arcaService, ArcaProperties arcaProperties) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.precioRepository = precioRepository;
        this.arcaService = arcaService;
        this.arcaProperties = arcaProperties;
    }

    // ------------------------------------------------------------------ LEER

    @Transactional(readOnly = true)
    public PaginaDto<FacturaResumenDto> buscar(FiltroFactura filtro, Pageable pageable) {
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new NegocioException("La fecha desde no puede ser mayor que la fecha hasta");
        }
        return PaginaDto.de(facturaRepository.findAll(FacturaFiltros.desde(filtro), pageable).map(FacturaResumenDto::de));
    }

    @Transactional(readOnly = true)
    public FacturaDto obtener(Long id) {
        return FacturaDto.de(buscarFactura(id));
    }

    // ------------------------------------------------------------------ CREAR

    @Transactional
    public FacturaDto crear(FacturaRequest request) {
        Factura factura = new Factura();
        factura.setPuntoVenta(arcaProperties.puntoVenta());
        cargarDatos(factura, request);
        return FacturaDto.de(facturaRepository.save(factura));
    }

    // ------------------------------------------------------------------ MODIFICAR

    @Transactional
    public FacturaDto actualizar(Long id, FacturaRequest request) {
        Factura factura = buscarFactura(id);
        verificarQueNoEsteInformada(factura);
        factura.getDetalles().clear(); // orphanRemoval borra los renglones viejos
        cargarDatos(factura, request);
        return FacturaDto.de(factura);
    }

    // ------------------------------------------------------------------ BORRAR

    @Transactional
    public void eliminar(Long id) {
        Factura factura = buscarFactura(id);
        verificarQueNoEsteInformada(factura);
        facturaRepository.delete(factura);
    }

    // ------------------------------------------------------------------ INFORMAR A ARCA

    /**
     * Manda la factura a ARCA.
     * - Si la aprueba: guarda numero + CAE y la factura queda BLOQUEADA para siempre.
     * - Si la rechaza: guarda las observaciones para que el usuario sepa que corregir.
     */
    @Transactional
    public FacturaDto informar(Long id) {
        Factura factura = buscarFactura(id);
        verificarQueNoEsteInformada(factura);

        ResultadoCae resultado = arcaService.autorizar(factura, totalesDe(factura));

        factura.setObservacionesArca(resultado.mensajes().isBlank() ? null : resultado.mensajes());
        if (resultado.aprobado()) {
            factura.setNumero(resultado.numero());
            factura.setCae(resultado.cae());
            factura.setCaeVencimiento(resultado.caeVencimiento());
            factura.setFechaInforme(LocalDateTime.now());
            factura.setInformadaArca(true);
        }
        return FacturaDto.de(factura);
    }

    // ------------------------------------------------------------------ ayudas privadas

    private Factura buscarFactura(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la factura " + id));
    }

    /** LA regla del TP: informada = bloqueada. */
    private void verificarQueNoEsteInformada(Factura factura) {
        if (factura.isInformadaArca()) {
            throw new FacturaBloqueadaException(factura.getId());
        }
    }

    /** Copia los datos del request a la factura. Los precios se buscan en la lista (no se confia en el front). */
    private void cargarDatos(Factura factura, FacturaRequest request) {
        if (!TipoComprobante.esValido(request.tipoComprobante())) {
            throw new NegocioException("Tipo de comprobante invalido. Use 1 (A), 6 (B) u 11 (C)");
        }
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new NoEncontradoException("No existe el cliente " + request.clienteId()));
        if (request.tipoComprobante() == TipoComprobante.FACTURA_A && cliente.getDocTipo() != 80) {
            throw new NegocioException("La Factura A solo se le puede hacer a un cliente con CUIT");
        }
        ListaPrecio lista = listaPrecioRepository.findById(request.listaPrecioId())
                .orElseThrow(() -> new NoEncontradoException("No existe la lista de precios " + request.listaPrecioId()));

        factura.setCliente(cliente);
        factura.setListaPrecio(lista);
        factura.setTipoComprobante(request.tipoComprobante());
        factura.setFecha(request.fecha() != null ? request.fecha() : LocalDate.now());

        for (FacturaRequest.Item item : request.items()) {
            ListaPrecioArticulo precio = precioRepository.buscarPrecio(lista.getId(), item.articuloId())
                    .orElseThrow(() -> new NegocioException(
                            "El articulo " + item.articuloId() + " no esta en la lista " + lista.getNombre()));

            FacturaDetalle detalle = new FacturaDetalle();
            detalle.setArticulo(precio.getArticulo());
            detalle.setDescripcion(precio.getArticulo().getDescripcion());
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precio.getPrecio());
            detalle.setAlicuotaIva(precio.getArticulo().getAlicuotaIva());
            detalle.setSubtotal(CalculadoraImportes.subtotal(item.cantidad(), precio.getPrecio()));
            factura.agregarDetalle(detalle);
        }

        Totales totales = totalesDe(factura);
        factura.setImporteNeto(totales.neto());
        factura.setImporteIva(totales.iva());
        factura.setImporteTotal(totales.total());
    }

    private Totales totalesDe(Factura factura) {
        List<Linea> lineas = factura.getDetalles().stream()
                .map(d -> new Linea(d.getSubtotal(), d.getAlicuotaIva()))
                .toList();
        return CalculadoraImportes.calcular(factura.getTipoComprobante(), lineas);
    }
}
