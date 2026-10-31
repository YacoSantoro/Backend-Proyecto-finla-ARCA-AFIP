package ar.edu.utn.facturacion.arca;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * WSAA = el "portero" de ARCA.
 * Le mostramos un papelito (TRA) firmado con nuestra clave privada y nos da token + sign.
 *
 * Importante: WSAA NO te da otro ticket si todavia tenes uno vigente
 * ("El CEE ya posee un TA valido..."). Por eso lo guardamos en un archivo y lo reusamos.
 */
@Component
public class WsaaClient {

    private static final String SERVICIO = "wsfe";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ArcaProperties props;
    private final SoapHttp soap;
    private TicketAcceso ticketEnMemoria;

    public WsaaClient(ArcaProperties props, SoapHttp soap) {
        this.props = props;
        this.soap = soap;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** synchronized = si dos pedidos llegan juntos, no le pedimos dos tickets a ARCA. */
    public synchronized TicketAcceso obtenerTicket() {
        if (ticketEnMemoria != null && ticketEnMemoria.vigente()) {
            return ticketEnMemoria;
        }
        TicketAcceso guardado = leerArchivo();
        if (guardado != null && guardado.vigente()) {
            ticketEnMemoria = guardado;
            return guardado;
        }
        TicketAcceso nuevo = pedirTicketNuevo();
        guardarArchivo(nuevo);
        ticketEnMemoria = nuevo;
        return nuevo;
    }

    private TicketAcceso pedirTicketNuevo() {
        // 1) Armamos el TRA (Ticket de Requerimiento de Acceso): "soy yo y quiero usar wsfe"
        String tra = armarTra();
        // 2) Lo firmamos con la clave privada + certificado (CMS / PKCS#7) y lo pasamos a Base64
        String cmsBase64 = firmar(tra);
        // 3) Lo mandamos dentro de un sobre SOAP al metodo loginCms
        String sobre = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" \
                xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <wsaa:loginCms><wsaa:in0>%s</wsaa:in0></wsaa:loginCms>
                  </soapenv:Body>
                </soapenv:Envelope>""".formatted(cmsBase64);

        String respuesta = soap.enviar(props.urlWsaa(), "", sobre);
        // 4) Leemos token, sign y vencimiento de la respuesta
        return parsearRespuesta(respuesta);
    }

    String armarTra() {
        ZonedDateTime ahora = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <loginTicketRequest version="1.0">
                  <header>
                    <uniqueId>%d</uniqueId>
                    <generationTime>%s</generationTime>
                    <expirationTime>%s</expirationTime>
                  </header>
                  <service>%s</service>
                </loginTicketRequest>""".formatted(
                ahora.toEpochSecond(),
                ahora.minusMinutes(10).format(FORMATO_FECHA),
                ahora.plusMinutes(10).format(FORMATO_FECHA),
                SERVICIO);
    }

    private String firmar(String tra) {
        try {
            PrivateKey clave = cargarClavePrivada(Path.of(props.clavePrivada()));
            X509Certificate certificado = cargarCertificado(Path.of(props.certificado()));

            ContentSigner firmante = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(clave);

            CMSSignedDataGenerator generador = new CMSSignedDataGenerator();
            generador.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build())
                    .build(firmante, certificado));
            generador.addCertificates(new JcaCertStore(List.of(certificado)));

            // true = el TRA va ADENTRO de la firma (ARCA lo necesita asi)
            CMSSignedData firmado = generador.generate(
                    new CMSProcessableByteArray(tra.getBytes(StandardCharsets.UTF_8)), true);
            return Base64.getEncoder().encodeToString(firmado.getEncoded());
        } catch (ArcaException e) {
            throw e;
        } catch (Exception e) {
            throw new ArcaException("No se pudo firmar el pedido para WSAA: " + e.getMessage(), e);
        }
    }

    static TicketAcceso parsearRespuesta(String respuestaSoap) {
        Document sobre = Xml.parsear(respuestaSoap);
        Xml.fallarSiHayFault(sobre, "WSAA");

        // loginCmsReturn trae OTRO xml adentro, escrito como texto. Lo volvemos a parsear.
        String ticketXml = Xml.texto(sobre, "loginCmsReturn");
        if (ticketXml == null) {
            throw new ArcaException("WSAA no devolvio el ticket: " + Xml.recortar(respuestaSoap));
        }
        Document ticket = Xml.parsear(ticketXml);
        String token = Xml.texto(ticket, "token");
        String sign = Xml.texto(ticket, "sign");
        Instant vence = OffsetDateTime.parse(Xml.texto(ticket, "expirationTime")).toInstant();
        return new TicketAcceso(token, sign, vence);
    }

    private static PrivateKey cargarClavePrivada(Path archivo) throws Exception {
        if (!Files.exists(archivo)) {
            throw new ArcaException("No encuentro la clave privada en " + archivo.toAbsolutePath());
        }
        // PEMParser entiende las dos formas que genera openssl: "BEGIN RSA PRIVATE KEY" y "BEGIN PRIVATE KEY"
        try (PEMParser lector = new PEMParser(new FileReader(archivo.toFile()))) {
            Object leido = lector.readObject();
            JcaPEMKeyConverter conversor = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (leido instanceof PEMKeyPair par) {
                return conversor.getKeyPair(par).getPrivate();
            }
            if (leido instanceof PrivateKeyInfo info) {
                return conversor.getPrivateKey(info);
            }
            throw new ArcaException("El archivo " + archivo + " no parece una clave privada PEM");
        }
    }

    private static X509Certificate cargarCertificado(Path archivo) throws Exception {
        if (!Files.exists(archivo)) {
            throw new ArcaException("No encuentro el certificado en " + archivo.toAbsolutePath());
        }
        try (InputStream in = new FileInputStream(archivo.toFile())) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    // ---------- Guardar / leer el ticket en un archivo ----------

    private Path archivoTicket() {
        String ambiente = props.produccion() ? "prod" : "homo";
        return Path.of(props.carpetaTickets(), "ticket-" + ambiente + "-" + props.cuit() + "-" + SERVICIO + ".properties");
    }

    private TicketAcceso leerArchivo() {
        Path archivo = archivoTicket();
        if (!Files.exists(archivo)) {
            return null;
        }
        try (InputStream in = new FileInputStream(archivo.toFile())) {
            Properties p = new Properties();
            p.load(in);
            return new TicketAcceso(p.getProperty("token"), p.getProperty("sign"),
                    Instant.ofEpochMilli(Long.parseLong(p.getProperty("expiracion"))));
        } catch (Exception e) {
            return null; // archivo roto: pedimos uno nuevo
        }
    }

    private void guardarArchivo(TicketAcceso ticket) {
        Path archivo = archivoTicket();
        try {
            Files.createDirectories(archivo.getParent());
            Properties p = new Properties();
            p.setProperty("token", ticket.token());
            p.setProperty("sign", ticket.sign());
            p.setProperty("expiracion", String.valueOf(ticket.expiracion().toEpochMilli()));
            try (OutputStream out = new FileOutputStream(archivo.toFile())) {
                p.store(out, "Ticket de acceso ARCA - " + SERVICIO);
            }
        } catch (Exception e) {
            // No es grave: solo perdemos el cache
            System.err.println("No se pudo guardar el ticket en " + archivo + ": " + e.getMessage());
        }
    }
}
