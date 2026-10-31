package ar.edu.utn.facturacion.arca;

import org.springframework.stereotype.Component;

/** WSFE = la "ventanilla" de facturacion. Usa WsfeXml para armar/leer y SoapHttp para mandar. */
@Component
public class WsfeClient {

    private final ArcaProperties props;
    private final SoapHttp soap;

    public WsfeClient(ArcaProperties props, SoapHttp soap) {
        this.props = props;
        this.soap = soap;
    }

    /** "Che ARCA, cual fue el ultimo numero que autorizaste para este punto de venta y tipo?" */
    public long ultimoAutorizado(TicketAcceso ticket, int puntoVenta, int tipoComprobante) {
        String pedido = WsfeXml.ultimoAutorizado(props.cuit(), ticket, puntoVenta, tipoComprobante);
        String respuesta = soap.enviar(props.urlWsfe(), WsfeXml.NS + "FECompUltimoAutorizado", pedido);
        return WsfeXml.parsearUltimoAutorizado(respuesta);
    }

    /** "Che ARCA, autorizame esta factura" -> CAE (o rechazo con observaciones). */
    public ResultadoCae solicitarCae(TicketAcceso ticket, SolicitudCae solicitud) {
        String pedido = WsfeXml.solicitarCae(props.cuit(), ticket, solicitud);
        String respuesta = soap.enviar(props.urlWsfe(), WsfeXml.NS + "FECAESolicitar", pedido);
        return WsfeXml.parsearCae(respuesta);
    }
}
