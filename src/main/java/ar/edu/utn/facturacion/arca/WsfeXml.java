package ar.edu.utn.facturacion.arca;

import ar.edu.utn.facturacion.service.CalculadoraImportes.IvaPorAlicuota;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Totales;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arma los XML que le mandamos a WSFE y lee los que nos devuelve.
 * Es todo texto: no se conecta a nada (por eso se puede testear sin internet).
 *
 * OJO: WSFE es estricto con el ORDEN de los tags dentro de FECAEDetRequest.
 * El orden de aca sale del WSDL (WSFEv1.wsdl -> FEDetRequest).
 */
public final class WsfeXml {

    static final String NS = "http://ar.gov.afip.dif.FEV1/";
    private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Codigos de ARCA para cada % de IVA. */
    private static final Map<String, Integer> ID_ALICUOTA = Map.of(
            "0.00", 3, "10.50", 4, "21.00", 5, "27.00", 6, "5.00", 8, "2.50", 9);

    private WsfeXml() {
    }

    // ---------------------------------------------------------------- pedidos

    public static String ultimoAutorizado(long cuit, TicketAcceso ticket, int puntoVenta, int tipoComprobante) {
        return sobre("""
                <ar:FECompUltimoAutorizado>
                  %s
                  <ar:PtoVta>%d</ar:PtoVta>
                  <ar:CbteTipo>%d</ar:CbteTipo>
                </ar:FECompUltimoAutorizado>""".formatted(auth(cuit, ticket), puntoVenta, tipoComprobante));
    }

    public static String solicitarCae(long cuit, TicketAcceso ticket, SolicitudCae s) {
        Totales t = s.totales();
        String fecha = s.fecha().format(AAAAMMDD);

        StringBuilder det = new StringBuilder();
        det.append(tag("Concepto", "1"));                       // 1 = Productos
        det.append(tag("DocTipo", s.docTipo()));
        det.append(tag("DocNro", s.docNro()));
        det.append(tag("CbteDesde", s.numero()));
        det.append(tag("CbteHasta", s.numero()));
        det.append(tag("CbteFch", fecha));
        det.append(tag("ImpTotal", monto(t.total())));
        det.append(tag("ImpTotConc", "0.00"));                  // no gravado
        det.append(tag("ImpNeto", monto(t.neto())));
        det.append(tag("ImpOpEx", "0.00"));                     // exento
        det.append(tag("ImpTrib", "0.00"));                     // otros tributos
        det.append(tag("ImpIVA", monto(t.iva())));
        det.append(tag("MonId", "PES"));                        // pesos argentinos
        det.append(tag("MonCotiz", "1"));
        det.append(tag("CondicionIVAReceptorId", s.condicionIvaReceptor())); // obligatorio (RG 5616)

        // Factura A y B: detalle del IVA por alicuota. Factura C: NO va este nodo.
        if (!t.ivas().isEmpty()) {
            det.append("<ar:Iva>");
            for (IvaPorAlicuota iva : t.ivas()) {
                det.append("<ar:AlicIva>")
                        .append(tag("Id", idAlicuota(iva.alicuota())))
                        .append(tag("BaseImp", monto(iva.baseImponible())))
                        .append(tag("Importe", monto(iva.importe())))
                        .append("</ar:AlicIva>");
            }
            det.append("</ar:Iva>");
        }

        return sobre("""
                <ar:FECAESolicitar>
                  %s
                  <ar:FeCAEReq>
                    <ar:FeCabReq>
                      <ar:CantReg>1</ar:CantReg>
                      <ar:PtoVta>%d</ar:PtoVta>
                      <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FeCabReq>
                    <ar:FeDetReq>
                      <ar:FECAEDetRequest>%s</ar:FECAEDetRequest>
                    </ar:FeDetReq>
                  </ar:FeCAEReq>
                </ar:FECAESolicitar>""".formatted(auth(cuit, ticket), s.puntoVenta(), s.tipoComprobante(), det));
    }

    // ---------------------------------------------------------------- respuestas

    public static long parsearUltimoAutorizado(String respuesta) {
        Document doc = Xml.parsear(respuesta);
        Xml.fallarSiHayFault(doc, "WSFE");
        List<String> errores = errores(doc);
        if (!errores.isEmpty()) {
            throw new ArcaException("WSFE no informo el ultimo comprobante: " + String.join(" | ", errores));
        }
        String nro = Xml.texto(doc, "CbteNro");
        if (nro == null) {
            throw new ArcaException("WSFE no devolvio CbteNro: " + Xml.recortar(respuesta));
        }
        return Long.parseLong(nro);
    }

    public static ResultadoCae parsearCae(String respuesta) {
        Document doc = Xml.parsear(respuesta);
        Xml.fallarSiHayFault(doc, "WSFE");

        List<String> errores = errores(doc);
        List<Element> detalles = Xml.elementos(doc, "FECAEDetResponse");

        if (detalles.isEmpty()) {
            // Ni siquiera llego a mirar el comprobante (ej: token vencido, CUIT mal)
            String resultadoCab = Xml.texto(doc, "Resultado");
            return new ResultadoCae(0, resultadoCab == null ? "R" : resultadoCab, null, null, List.of(), errores);
        }

        Element det = detalles.get(0);
        List<String> observaciones = new ArrayList<>();
        for (Element obs : Xml.elementos(det, "Obs")) {
            observaciones.add("[" + Xml.texto(obs, "Code") + "] " + Xml.texto(obs, "Msg"));
        }
        String vto = Xml.texto(det, "CAEFchVto");
        String cae = Xml.texto(det, "CAE");
        return new ResultadoCae(
                Long.parseLong(Xml.texto(det, "CbteDesde")),
                Xml.texto(det, "Resultado"),
                cae == null || cae.isBlank() ? null : cae,
                vto == null || vto.isBlank() ? null : LocalDate.parse(vto, AAAAMMDD),
                observaciones,
                errores);
    }

    // ---------------------------------------------------------------- ayudas

    private static List<String> errores(Document doc) {
        List<String> lista = new ArrayList<>();
        for (Element err : Xml.elementos(doc, "Err")) {
            lista.add("[" + Xml.texto(err, "Code") + "] " + Xml.texto(err, "Msg"));
        }
        return lista;
    }

    private static String sobre(String cuerpo) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                %s
                  </soapenv:Body>
                </soapenv:Envelope>""".formatted(NS, cuerpo);
    }

    private static String auth(long cuit, TicketAcceso ticket) {
        return "<ar:Auth>" + tag("Token", ticket.token()) + tag("Sign", ticket.sign()) + tag("Cuit", cuit) + "</ar:Auth>";
    }

    private static String tag(String nombre, Object valor) {
        return "<ar:" + nombre + ">" + valor + "</ar:" + nombre + ">";
    }

    private static String monto(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static int idAlicuota(BigDecimal alicuota) {
        Integer id = ID_ALICUOTA.get(alicuota.setScale(2, RoundingMode.HALF_UP).toPlainString());
        if (id == null) {
            throw new IllegalArgumentException("Alicuota de IVA no soportada por ARCA: " + alicuota + "%");
        }
        return id;
    }
}
