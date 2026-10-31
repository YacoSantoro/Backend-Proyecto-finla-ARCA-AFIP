package ar.edu.utn.facturacion.arca;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Ayudas chiquitas para leer las respuestas XML de ARCA. */
final class Xml {

    private Xml() {
    }

    static Document parsear(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ArcaException("ARCA devolvio algo que no es XML valido: " + recortar(xml), e);
        }
    }

    /** Texto del primer tag con ese nombre (sin importar el namespace), o null si no esta. */
    static String texto(Node donde, String tag) {
        NodeList nodos = donde instanceof Document doc
                ? doc.getElementsByTagNameNS("*", tag)
                : ((Element) donde).getElementsByTagNameNS("*", tag);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().trim();
    }

    static List<Element> elementos(Node donde, String tag) {
        NodeList nodos = donde instanceof Document doc
                ? doc.getElementsByTagNameNS("*", tag)
                : ((Element) donde).getElementsByTagNameNS("*", tag);
        List<Element> lista = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            lista.add((Element) nodos.item(i));
        }
        return lista;
    }

    /** Si ARCA respondio un "SOAP Fault" (error del servidor), lo convertimos en excepcion. */
    static void fallarSiHayFault(Document doc, String servicio) {
        String fault = texto(doc, "faultstring");
        if (fault != null) {
            throw new ArcaException(servicio + " respondio con error: " + fault);
        }
    }

    static String recortar(String texto) {
        if (texto == null) {
            return "(vacio)";
        }
        return texto.length() > 500 ? texto.substring(0, 500) + "..." : texto;
    }
}
