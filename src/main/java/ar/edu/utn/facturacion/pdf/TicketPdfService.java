package ar.edu.utn.facturacion.pdf;

import ar.edu.utn.facturacion.arca.ArcaProperties;
import ar.edu.utn.facturacion.entity.Cliente;
import ar.edu.utn.facturacion.entity.Factura;
import ar.edu.utn.facturacion.entity.FacturaDetalle;
import ar.edu.utn.facturacion.repository.FacturaRepository;
import ar.edu.utn.facturacion.service.NegocioException;
import ar.edu.utn.facturacion.service.NoEncontradoException;
import ar.edu.utn.facturacion.service.TipoComprobante;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

/**
 * Arma el ticket en PDF (ancho 80 mm, para impresora termica o cualquier impresora)
 * de una factura YA informada a ARCA: con CAE, vencimiento y el codigo QR que pide ARCA.
 */
@Service
public class TicketPdfService {

    /** 80 mm en puntos PDF (1 mm = 72 / 25.4 pt). */
    private static final float ANCHO = 80 * 72 / 25.4f;
    private static final float MARGEN = 10;

    private static final Font CHICA = new Font(Font.HELVETICA, 7);
    private static final Font NORMAL = new Font(Font.HELVETICA, 8);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font TITULO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font TOTAL = new Font(Font.HELVETICA, 12, Font.BOLD);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat PESOS = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

    private static final Map<Integer, String> LETRA = Map.of(1, "A", 6, "B", 11, "C");
    private static final Map<Integer, String> COND_IVA = Map.of(
            1, "IVA Responsable Inscripto", 4, "IVA Sujeto Exento", 5, "Consumidor Final", 6, "Responsable Monotributo");

    private final FacturaRepository facturaRepository;
    private final ArcaProperties arca;
    private final EmisorProperties emisor;

    public TicketPdfService(FacturaRepository facturaRepository, ArcaProperties arca, EmisorProperties emisor) {
        this.facturaRepository = facturaRepository;
        this.arca = arca;
        this.emisor = emisor;
    }

    @Transactional(readOnly = true)
    public byte[] generar(Long id) {
        Factura f = facturaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la factura " + id));
        if (!f.isInformadaArca()) {
            throw new NegocioException("Solo se puede imprimir el ticket de una factura informada a ARCA (con CAE)");
        }

        // El alto depende de la cantidad de renglones: asi no sobra papel en la termica
        float alto = 560 + f.getDetalles().size() * 30;
        Document doc = new Document(new Rectangle(ANCHO, alto), MARGEN, MARGEN, MARGEN, MARGEN);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, salida);
        doc.addTitle("Ticket " + nombreArchivo(f));
        doc.open();

        encabezado(doc, f);
        linea(doc);
        receptor(doc, f.getCliente());
        linea(doc);
        renglones(doc, f);
        linea(doc);
        totales(doc, f);
        linea(doc);
        pie(doc, f);

        doc.close();
        return salida.toByteArray();
    }

    /** "FC-C-0001-00000008" (para el nombre del archivo). */
    public String nombreArchivo(Factura f) {
        return "FC-" + LETRA.getOrDefault(f.getTipoComprobante(), "X") + "-" + numero(f);
    }

    @Transactional(readOnly = true)
    public String nombreArchivo(Long id) {
        return facturaRepository.findById(id).map(this::nombreArchivo).orElse("ticket");
    }

    // ------------------------------------------------------------------ secciones

    private void encabezado(Document doc, Factura f) {
        centrado(doc, nvl(emisor.razonSocial()), TITULO);
        centrado(doc, "CUIT: " + arca.cuit(), NORMAL);
        centrado(doc, nvl(emisor.domicilio()), CHICA);
        centrado(doc, nvl(emisor.condicionIva()), CHICA);
        if (!nvl(emisor.ingresosBrutos()).isBlank()) centrado(doc, "Ing. Brutos: " + emisor.ingresosBrutos(), CHICA);
        if (!nvl(emisor.inicioActividades()).isBlank()) centrado(doc, "Inicio de actividades: " + emisor.inicioActividades(), CHICA);
        linea(doc);

        String letra = LETRA.getOrDefault(f.getTipoComprobante(), "?");
        centrado(doc, "FACTURA " + letra, TITULO);
        centrado(doc, "Cód. " + String.format("%03d", f.getTipoComprobante()) + " - ORIGINAL", CHICA);
        clave(doc, "Nro.", numero(f));
        clave(doc, "Fecha", f.getFecha().format(FECHA));
    }

    private void receptor(Document doc, Cliente c) {
        clave(doc, "Cliente", c.getRazonSocial());
        clave(doc, documento(c), "");
        clave(doc, "Cond. IVA", COND_IVA.getOrDefault(c.getCondicionIva(), String.valueOf(c.getCondicionIva())));
        if (c.getDireccion() != null && !c.getDireccion().isBlank()) clave(doc, "Domicilio", c.getDireccion());
    }

    private void renglones(Document doc, Factura f) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 3, 2});
        t.setWidthPercentage(100);
        t.addCell(celda("Cant.", NEGRITA, Element.ALIGN_LEFT));
        t.addCell(celda("Descripción", NEGRITA, Element.ALIGN_LEFT));
        t.addCell(celda("Importe", NEGRITA, Element.ALIGN_RIGHT));
        for (FacturaDetalle d : f.getDetalles()) {
            t.addCell(celda(cantidad(d.getCantidad()), NORMAL, Element.ALIGN_LEFT));
            String desc = d.getDescripcion() + "\n" + "x " + dinero(d.getPrecioUnitario())
                    + (f.getTipoComprobante() == TipoComprobante.FACTURA_A ? "  (IVA " + cantidad(d.getAlicuotaIva()) + "%)" : "");
            t.addCell(celda(desc, NORMAL, Element.ALIGN_LEFT));
            t.addCell(celda(dinero(d.getSubtotal()), NORMAL, Element.ALIGN_RIGHT));
        }
        doc.add(t);
    }

    private void totales(Document doc, Factura f) {
        if (f.getTipoComprobante() == TipoComprobante.FACTURA_A) {
            clave(doc, "Neto gravado", dinero(f.getImporteNeto()));
            clave(doc, "IVA", dinero(f.getImporteIva()));
        }
        PdfPTable t = new PdfPTable(new float[]{1, 1});
        t.setWidthPercentage(100);
        t.addCell(celda("TOTAL", TOTAL, Element.ALIGN_LEFT));
        t.addCell(celda(dinero(f.getImporteTotal()), TOTAL, Element.ALIGN_RIGHT));
        doc.add(t);
        if (f.getTipoComprobante() == TipoComprobante.FACTURA_B) {
            // Ley 27.743 (Transparencia fiscal al consumidor)
            Paragraph p = new Paragraph("IVA contenido: " + dinero(f.getImporteIva()), CHICA);
            doc.add(p);
        }
    }

    private void pie(Document doc, Factura f) {
        clave(doc, "CAE", f.getCae());
        clave(doc, "Vto. CAE", f.getCaeVencimiento() != null ? f.getCaeVencimiento().format(FECHA) : "-");

        Image qr = qr(urlQr(f));
        qr.scaleAbsolute(110, 110);
        qr.setAlignment(Element.ALIGN_CENTER);
        doc.add(qr);
        centrado(doc, "Comprobante Autorizado por ARCA", NEGRITA);
        if (!arca.produccion()) centrado(doc, "(Homologación - sin validez fiscal)", CHICA);
        centrado(doc, "¡Gracias por su compra!", CHICA);
    }

    // ------------------------------------------------------------------ QR de ARCA

    /**
     * Especificacion oficial: https://www.afip.gob.ar/fe/qr/especificaciones.asp
     * URL = https://www.afip.gob.ar/fe/qr/?p= + Base64(JSON con los datos del comprobante)
     */
    String urlQr(Factura f) {
        Cliente c = f.getCliente();
        String json = "{"
                + "\"ver\":1,"
                + "\"fecha\":\"" + f.getFecha() + "\","
                + "\"cuit\":" + arca.cuit() + ","
                + "\"ptoVta\":" + f.getPuntoVenta() + ","
                + "\"tipoCmp\":" + f.getTipoComprobante() + ","
                + "\"nroCmp\":" + f.getNumero() + ","
                + "\"importe\":" + f.getImporteTotal().setScale(2).toPlainString() + ","
                + "\"moneda\":\"PES\","
                + "\"ctz\":1,"
                + "\"tipoDocRec\":" + c.getDocTipo() + ","
                + "\"nroDocRec\":" + c.getDocNro() + ","
                + "\"tipoCodAut\":\"E\","
                + "\"codAut\":" + f.getCae()
                + "}";
        return "https://www.afip.gob.ar/fe/qr/?p="
                + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private Image qr(String texto) {
        try {
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 300, 300, Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage img = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return Image.getInstance(img, null);
        } catch (WriterException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    // ------------------------------------------------------------------ ayudas

    private String numero(Factura f) {
        return String.format("%04d-%08d", f.getPuntoVenta(), f.getNumero() == null ? 0 : f.getNumero());
    }

    private static String documento(Cliente c) {
        return switch (c.getDocTipo()) {
            case 80 -> "CUIT: " + c.getDocNro();
            case 96 -> "DNI: " + c.getDocNro();
            case 99 -> "Doc.: Sin identificar";
            default -> "Doc. " + c.getDocTipo() + ": " + c.getDocNro();
        };
    }

    private static String dinero(BigDecimal v) {
        synchronized (PESOS) {
            return PESOS.format(v == null ? BigDecimal.ZERO : v);
        }
    }

    private static String cantidad(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static void centrado(Document doc, String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setLeading(font.getSize() + 2);
        doc.add(p);
    }

    /** "Etiqueta: valor" en una linea. */
    private static void clave(Document doc, String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.setLeading(10);
        p.add(new Chunk(etiqueta + (valor.isEmpty() ? "" : ": "), NEGRITA));
        p.add(new Phrase(valor, NORMAL));
        doc.add(p);
    }

    private static void linea(Document doc) {
        LineSeparator ls = new LineSeparator(0.5f, 100, Color.GRAY, Element.ALIGN_CENTER, -2);
        Paragraph p = new Paragraph(new Chunk(ls));
        p.setSpacingBefore(2);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static PdfPCell celda(String texto, Font font, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(alineacion);
        c.setPaddingLeft(0);
        c.setPaddingRight(0);
        c.setPaddingBottom(3);
        return c;
    }
}
