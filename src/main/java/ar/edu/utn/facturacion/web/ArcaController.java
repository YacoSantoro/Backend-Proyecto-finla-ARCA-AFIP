package ar.edu.utn.facturacion.web;

import ar.edu.utn.facturacion.arca.ArcaProperties;
import ar.edu.utn.facturacion.arca.ArcaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Para chequear la conexion con ARCA sin tener que hacer una factura. */
@RestController
@RequestMapping("/api/arca")
public class ArcaController {

    private final ArcaProperties props;
    private final ArcaService arcaService;

    public ArcaController(ArcaProperties props, ArcaService arcaService) {
        this.props = props;
        this.arcaService = arcaService;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of(
                "simulado", props.simulado(),
                "ambiente", props.produccion() ? "PRODUCCION" : "HOMOLOGACION",
                "cuit", props.cuit(),
                "puntoVenta", props.puntoVenta());
    }

    /** Ej: GET /api/arca/ultimo/11 -> ultimo numero de Factura C autorizado. */
    @GetMapping("/ultimo/{tipoComprobante}")
    public Map<String, Object> ultimo(@PathVariable int tipoComprobante) {
        return Map.of("tipoComprobante", tipoComprobante,
                "ultimoNumero", arcaService.ultimoAutorizado(tipoComprobante));
    }
}
