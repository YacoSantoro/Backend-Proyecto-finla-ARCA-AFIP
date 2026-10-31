package ar.edu.utn.facturacion.arca;

import ar.edu.utn.facturacion.service.CalculadoraImportes;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Linea;
import ar.edu.utn.facturacion.service.TipoComprobante;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WsfeXmlTest {

    private final TicketAcceso ticket = new TicketAcceso("TOKEN123", "SIGN456", Instant.now().plusSeconds(3600));

    private SolicitudCae solicitud(int tipo) {
        var totales = CalculadoraImportes.calcular(tipo, List.of(new Linea(new BigDecimal("1210.00"), new BigDecimal("21"))));
        return new SolicitudCae(1, tipo, 8, LocalDate.of(2026, 10, 31), 99, 0, 5, totales);
    }

    @Test
    void facturaB_llevaNodoIva_despuesDeCondicionIva() {
        String xml = WsfeXml.solicitarCae(20111111112L, ticket, solicitud(TipoComprobante.FACTURA_B));

        assertThat(xml).contains("<ar:Token>TOKEN123</ar:Token>", "<ar:Cuit>20111111112</ar:Cuit>",
                "<ar:CbteTipo>6</ar:CbteTipo>", "<ar:CbteDesde>8</ar:CbteDesde>", "<ar:CbteFch>20261031</ar:CbteFch>",
                "<ar:ImpNeto>1000.00</ar:ImpNeto>", "<ar:ImpIVA>210.00</ar:ImpIVA>", "<ar:Id>5</ar:Id>");
        assertThat(xml.indexOf("<ar:MonId>")).isGreaterThan(xml.indexOf("<ar:ImpIVA>"));
        assertThat(xml.indexOf("<ar:Iva>")).isGreaterThan(xml.indexOf("<ar:CondicionIVAReceptorId>"));
    }

    @Test
    void facturaC_noLlevaNodoIva() {
        String xml = WsfeXml.solicitarCae(20111111112L, ticket, solicitud(TipoComprobante.FACTURA_C));

        assertThat(xml).contains("<ar:ImpNeto>1210.00</ar:ImpNeto>", "<ar:ImpIVA>0.00</ar:ImpIVA>")
                .doesNotContain("<ar:Iva>");
    }

    @Test
    void leeUnaRespuestaAprobada() {
        String respuesta = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <FECAESolicitarResponse xmlns="http://ar.gov.afip.dif.FEV1/"><FECAESolicitarResult>
                  <FeCabResp><Cuit>20111111112</Cuit><PtoVta>1</PtoVta><CbteTipo>11</CbteTipo><Resultado>A</Resultado></FeCabResp>
                  <FeDetResp><FECAEDetResponse>
                    <Concepto>1</Concepto><DocTipo>99</DocTipo><DocNro>0</DocNro>
                    <CbteDesde>8</CbteDesde><CbteHasta>8</CbteHasta><CbteFch>20261031</CbteFch>
                    <Resultado>A</Resultado><CAE>76441234567890</CAE><CAEFchVto>20261110</CAEFchVto>
                  </FECAEDetResponse></FeDetResp>
                </FECAESolicitarResult></FECAESolicitarResponse>
                </soap:Body></soap:Envelope>""";

        ResultadoCae r = WsfeXml.parsearCae(respuesta);

        assertThat(r.aprobado()).isTrue();
        assertThat(r.numero()).isEqualTo(8);
        assertThat(r.cae()).isEqualTo("76441234567890");
        assertThat(r.caeVencimiento()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void leeUnaRespuestaRechazadaConObservaciones() {
        String respuesta = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <FECAESolicitarResponse xmlns="http://ar.gov.afip.dif.FEV1/"><FECAESolicitarResult>
                  <FeCabResp><Resultado>R</Resultado></FeCabResp>
                  <FeDetResp><FECAEDetResponse>
                    <CbteDesde>8</CbteDesde><Resultado>R</Resultado>
                    <Observaciones><Obs><Code>10016</Code><Msg>El numero no es el proximo a autorizar</Msg></Obs></Observaciones>
                    <CAE></CAE><CAEFchVto></CAEFchVto>
                  </FECAEDetResponse></FeDetResp>
                </FECAESolicitarResult></FECAESolicitarResponse>
                </soap:Body></soap:Envelope>""";

        ResultadoCae r = WsfeXml.parsearCae(respuesta);

        assertThat(r.aprobado()).isFalse();
        assertThat(r.cae()).isNull();
        assertThat(r.mensajes()).contains("[10016]");
    }

    @Test
    void leeErroresGenerales_sinDetalle() {
        String respuesta = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <FECAESolicitarResponse xmlns="http://ar.gov.afip.dif.FEV1/"><FECAESolicitarResult>
                  <Errors><Err><Code>600</Code><Msg>ValidacionDeToken: No aparecio CUIT en lista de relaciones</Msg></Err></Errors>
                </FECAESolicitarResult></FECAESolicitarResponse>
                </soap:Body></soap:Envelope>""";

        ResultadoCae r = WsfeXml.parsearCae(respuesta);

        assertThat(r.aprobado()).isFalse();
        assertThat(r.errores()).containsExactly("[600] ValidacionDeToken: No aparecio CUIT en lista de relaciones");
    }

    @Test
    void ultimoAutorizado_okYConError() {
        String ok = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <FECompUltimoAutorizadoResponse xmlns="http://ar.gov.afip.dif.FEV1/"><FECompUltimoAutorizadoResult>
                <PtoVta>1</PtoVta><CbteTipo>11</CbteTipo><CbteNro>7</CbteNro>
                </FECompUltimoAutorizadoResult></FECompUltimoAutorizadoResponse></soap:Body></soap:Envelope>""";
        String error = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
                <FECompUltimoAutorizadoResponse xmlns="http://ar.gov.afip.dif.FEV1/"><FECompUltimoAutorizadoResult>
                <Errors><Err><Code>11002</Code><Msg>El punto de venta no se encuentra habilitado</Msg></Err></Errors>
                </FECompUltimoAutorizadoResult></FECompUltimoAutorizadoResponse></soap:Body></soap:Envelope>""";

        assertThat(WsfeXml.parsearUltimoAutorizado(ok)).isEqualTo(7);
        assertThatThrownBy(() -> WsfeXml.parsearUltimoAutorizado(error))
                .isInstanceOf(ArcaException.class).hasMessageContaining("11002");
    }

    @Test
    void soapFault_seConvierteEnArcaException() {
        String fault = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"><soapenv:Body>
                <soapenv:Fault><faultcode>ns1:coe.alreadyAuthenticated</faultcode>
                <faultstring>El CEE ya posee un TA valido para el acceso al WSN solicitado</faultstring></soapenv:Fault>
                </soapenv:Body></soapenv:Envelope>""";

        assertThatThrownBy(() -> WsaaClient.parsearRespuesta(fault))
                .isInstanceOf(ArcaException.class).hasMessageContaining("ya posee un TA valido");
    }

    @Test
    void wsaa_leeTokenYSignDelXmlEscapado() {
        String respuesta = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"><soapenv:Body>
                <loginCmsResponse xmlns="http://wsaa.view.sua.dvadac.desein.afip.gov"><loginCmsReturn>\
                &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;loginTicketResponse version="1.0"&gt;\
                &lt;header&gt;&lt;expirationTime&gt;2026-10-31T23:00:00.000-03:00&lt;/expirationTime&gt;&lt;/header&gt;\
                &lt;credentials&gt;&lt;token&gt;PD94bWw=&lt;/token&gt;&lt;sign&gt;abc+/=&lt;/sign&gt;&lt;/credentials&gt;\
                &lt;/loginTicketResponse&gt;</loginCmsReturn></loginCmsResponse>
                </soapenv:Body></soapenv:Envelope>""";

        TicketAcceso t = WsaaClient.parsearRespuesta(respuesta);

        assertThat(t.token()).isEqualTo("PD94bWw=");
        assertThat(t.sign()).isEqualTo("abc+/=");
        assertThat(t.expiracion()).isEqualTo(Instant.parse("2026-11-01T02:00:00Z"));
    }

    @Test
    void alicuotaDesconocida_falla() {
        assertThat(WsfeXml.idAlicuota(new BigDecimal("10.5"))).isEqualTo(4);
        assertThatThrownBy(() -> WsfeXml.idAlicuota(new BigDecimal("13")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
