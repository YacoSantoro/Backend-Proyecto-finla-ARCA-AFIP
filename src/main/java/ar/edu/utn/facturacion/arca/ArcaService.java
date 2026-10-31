package ar.edu.utn.facturacion.arca;

import ar.edu.utn.facturacion.entity.Cliente;
import ar.edu.utn.facturacion.entity.Factura;
import ar.edu.utn.facturacion.repository.FacturaRepository;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Totales;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * La "puerta de entrada" a ARCA para el resto del sistema.
 * Hace los 3 pasos: 1) ticket (WSAA)  2) ultimo numero (WSFE)  3) pedir CAE (WSFE).
 */
@Service
public class ArcaService {

    private final ArcaProperties props;
    private final WsaaClient wsaa;
    private final WsfeClient wsfe;
    private final FacturaRepository facturaRepository;

    public ArcaService(ArcaProperties props, WsaaClient wsaa, WsfeClient wsfe, FacturaRepository facturaRepository) {
        this.props = props;
        this.wsaa = wsaa;
        this.wsfe = wsfe;
        this.facturaRepository = facturaRepository;
    }

    public ResultadoCae autorizar(Factura factura, Totales totales) {
        if (props.simulado()) {
            return simular(factura);
        }
        // Paso 1: login
        TicketAcceso ticket = wsaa.obtenerTicket();
        // Paso 2: numero que toca = ultimo autorizado + 1
        long numero = wsfe.ultimoAutorizado(ticket, factura.getPuntoVenta(), factura.getTipoComprobante()) + 1;
        // Paso 3: pedir el CAE
        Cliente c = factura.getCliente();
        SolicitudCae solicitud = new SolicitudCae(factura.getPuntoVenta(), factura.getTipoComprobante(), numero,
                factura.getFecha(), c.getDocTipo(), c.getDocNro(), c.getCondicionIva(), totales);
        return wsfe.solicitarCae(ticket, solicitud);
    }

    /** Para probar sin internet ni certificado. Inventa un CAE de 14 digitos. */
    private ResultadoCae simular(Factura factura) {
        long numero = facturaRepository.ultimoNumero(factura.getPuntoVenta(), factura.getTipoComprobante()).orElse(0L) + 1;
        String cae = String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L));
        return new ResultadoCae(numero, "A", cae, factura.getFecha().plusDays(10),
                List.of("MODO SIMULADO: este CAE es inventado, no se envio nada a ARCA"), List.of());
    }

    /** Para el boton "probar conexion": si esto anda, el certificado y la delegacion estan bien. */
    public long ultimoAutorizado(int tipoComprobante) {
        if (props.simulado()) {
            return facturaRepository.ultimoNumero(props.puntoVenta(), tipoComprobante).orElse(0L);
        }
        return wsfe.ultimoAutorizado(wsaa.obtenerTicket(), props.puntoVenta(), tipoComprobante);
    }
}
