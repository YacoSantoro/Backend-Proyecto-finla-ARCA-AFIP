package ar.edu.utn.facturacion.service;

import ar.edu.utn.facturacion.service.CalculadoraImportes.Linea;
import ar.edu.utn.facturacion.service.CalculadoraImportes.Totales;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraImportesTest {

    private static Linea linea(String subtotal, String alicuota) {
        return new Linea(new BigDecimal(subtotal), new BigDecimal(alicuota));
    }

    @Test
    void facturaC_noDiscriminaIva_netoIgualAlTotal() {
        Totales t = CalculadoraImportes.calcular(TipoComprobante.FACTURA_C,
                List.of(linea("1210.00", "21"), linea("500.00", "10.5")));

        assertThat(t.total()).isEqualByComparingTo("1710.00");
        assertThat(t.neto()).isEqualByComparingTo("1710.00");
        assertThat(t.iva()).isEqualByComparingTo("0");
        assertThat(t.ivas()).isEmpty();
    }

    @Test
    void facturaB_sacaElIvaDelPrecioFinal() {
        Totales t = CalculadoraImportes.calcular(TipoComprobante.FACTURA_B, List.of(linea("1210.00", "21")));

        assertThat(t.neto()).isEqualByComparingTo("1000.00");
        assertThat(t.iva()).isEqualByComparingTo("210.00");
        assertThat(t.total()).isEqualByComparingTo("1210.00");
        assertThat(t.ivas()).hasSize(1);
        assertThat(t.ivas().get(0).baseImponible()).isEqualByComparingTo("1000.00");
    }

    @Test
    void facturaA_agrupaPorAlicuota_yNetoMasIvaDaElTotal() {
        Totales t = CalculadoraImportes.calcular(TipoComprobante.FACTURA_A,
                List.of(linea("1210.00", "21"), linea("605.00", "21"), linea("1105.00", "10.5")));

        assertThat(t.ivas()).hasSize(2);
        assertThat(t.ivas().get(0).alicuota()).isEqualByComparingTo("10.5");
        assertThat(t.ivas().get(0).baseImponible()).isEqualByComparingTo("1000.00");
        assertThat(t.ivas().get(1).baseImponible()).isEqualByComparingTo("1500.00");
        assertThat(t.neto().add(t.iva())).isEqualByComparingTo(t.total());
    }

    @Test
    void redondeo_netoMasIvaSiempreCierraConElTotal() {
        Totales t = CalculadoraImportes.calcular(TipoComprobante.FACTURA_B, List.of(linea("99.99", "21")));

        assertThat(t.neto()).isEqualByComparingTo("82.64");
        assertThat(t.iva()).isEqualByComparingTo("17.35");
        assertThat(t.neto().add(t.iva())).isEqualByComparingTo("99.99");
    }

    @Test
    void subtotal_multiplicaYRedondeaADosDecimales() {
        assertThat(CalculadoraImportes.subtotal(new BigDecimal("3"), new BigDecimal("3500")))
                .isEqualByComparingTo("10500.00");
        assertThat(CalculadoraImportes.subtotal(new BigDecimal("0.333"), new BigDecimal("10")))
                .isEqualByComparingTo("3.33");
    }
}
