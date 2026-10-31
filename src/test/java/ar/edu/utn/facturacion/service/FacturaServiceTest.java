package ar.edu.utn.facturacion.service;

import ar.edu.utn.facturacion.dto.FacturaDto;
import ar.edu.utn.facturacion.dto.FacturaRequest;
import ar.edu.utn.facturacion.dto.FacturaResumenDto;
import ar.edu.utn.facturacion.dto.FiltroFactura;
import ar.edu.utn.facturacion.dto.PaginaDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa una base H2 en memoria (se borra al terminar) y ARCA en modo simulado. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:facturas-test;DB_CLOSE_DELAY=-1",
        "arca.simulado=true",
        "arca.punto-venta=1"
})
class FacturaServiceTest {

    @Autowired
    private FacturaService service;

    private FacturaRequest pedido(long clienteId, int tipo, long articuloId, String cantidad) {
        return new FacturaRequest(clienteId, 1L, tipo, LocalDate.now(),
                List.of(new FacturaRequest.Item(articuloId, new BigDecimal(cantidad))));
    }

    @Test
    void crear_tomaElPrecioDeLaLista_yCalculaTotales() {
        // Articulo 2 en lista Minorista = 3500
        FacturaDto f = service.crear(pedido(2, TipoComprobante.FACTURA_C, 2, "3"));

        assertThat(f.id()).isNotNull();
        assertThat(f.numero()).isNull();
        assertThat(f.informadaArca()).isFalse();
        assertThat(f.items().get(0).precioUnitario()).isEqualByComparingTo("3500");
        assertThat(f.importeTotal()).isEqualByComparingTo("10500.00");
    }

    @Test
    void facturaInformada_quedaBloqueada() {
        FacturaDto f = service.crear(pedido(2, TipoComprobante.FACTURA_C, 1, "1"));

        FacturaDto informada = service.informar(f.id());

        assertThat(informada.informadaArca()).isTrue();
        assertThat(informada.cae()).hasSize(14);
        assertThat(informada.numero()).isPositive();
        assertThatThrownBy(() -> service.actualizar(f.id(), pedido(2, TipoComprobante.FACTURA_C, 1, "2")))
                .isInstanceOf(FacturaBloqueadaException.class);
        assertThatThrownBy(() -> service.eliminar(f.id()))
                .isInstanceOf(FacturaBloqueadaException.class);
        assertThatThrownBy(() -> service.informar(f.id()))
                .isInstanceOf(FacturaBloqueadaException.class);
    }

    @Test
    void facturaNoInformada_sePuedeModificarYBorrar() {
        FacturaDto f = service.crear(pedido(3, TipoComprobante.FACTURA_B, 1, "1"));

        FacturaDto modificada = service.actualizar(f.id(), pedido(3, TipoComprobante.FACTURA_B, 1, "2"));
        assertThat(modificada.items()).hasSize(1);
        assertThat(modificada.importeTotal()).isEqualByComparingTo("24000.00");

        service.eliminar(f.id());
        assertThatThrownBy(() -> service.obtener(f.id())).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void facturaA_aConsumidorFinalSinCuit_falla() {
        assertThatThrownBy(() -> service.crear(pedido(2, TipoComprobante.FACTURA_A, 1, "1")))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void filtros_porClienteEInformada() {
        FacturaDto informada = service.crear(pedido(8, TipoComprobante.FACTURA_C, 5, "1"));
        service.informar(informada.id());
        service.crear(pedido(8, TipoComprobante.FACTURA_C, 5, "2"));

        PaginaDto<FacturaResumenDto> soloInformadas = service.buscar(
                new FiltroFactura(null, null, null, "carlos", true), PageRequest.of(0, 10));
        PaginaDto<FacturaResumenDto> todasDeCarlos = service.buscar(
                new FiltroFactura(null, null, null, "carlos", null), PageRequest.of(0, 10));

        assertThat(soloInformadas.contenido()).extracting(FacturaResumenDto::id).containsExactly(informada.id());
        assertThat(todasDeCarlos.totalElementos()).isEqualTo(2);
    }

    @Test
    void filtros_fechaDesdeMayorQueHasta_falla() {
        FiltroFactura malo = new FiltroFactura(null, LocalDate.now(), LocalDate.now().minusDays(1), null, null);
        assertThatThrownBy(() -> service.buscar(malo, PageRequest.of(0, 10))).isInstanceOf(NegocioException.class);
    }
}
