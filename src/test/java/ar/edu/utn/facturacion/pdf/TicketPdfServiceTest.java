package ar.edu.utn.facturacion.pdf;

import ar.edu.utn.facturacion.dto.FacturaDto;
import ar.edu.utn.facturacion.dto.FacturaRequest;
import ar.edu.utn.facturacion.service.FacturaService;
import ar.edu.utn.facturacion.service.NegocioException;
import ar.edu.utn.facturacion.service.TipoComprobante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ticket-test;DB_CLOSE_DELAY=-1",
        "arca.simulado=true",
        "arca.punto-venta=1"
})
class TicketPdfServiceTest {

    @Autowired
    private FacturaService facturas;

    @Autowired
    private TicketPdfService tickets;

    private FacturaDto crear(int tipo) {
        return facturas.crear(new FacturaRequest(2L, 1L, tipo, LocalDate.now(),
                List.of(new FacturaRequest.Item(1L, new BigDecimal("2")),
                        new FacturaRequest.Item(2L, new BigDecimal("1")))));
    }

    @Test
    void facturaInformada_generaUnPdf() throws Exception {
        FacturaDto f = facturas.informar(crear(TipoComprobante.FACTURA_B).id());

        byte[] pdf = tickets.generar(f.id());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(tickets.nombreArchivo(f.id())).startsWith("FC-B-0001-");
        // Queda en target/ para poder abrirlo y mirarlo
        Files.write(Path.of("target", "ticket-ejemplo.pdf"), pdf);
    }

    @Test
    void facturaSinInformar_noTieneTicket() {
        FacturaDto f = crear(TipoComprobante.FACTURA_C);
        assertThatThrownBy(() -> tickets.generar(f.id())).isInstanceOf(NegocioException.class);
    }
}
