package ar.edu.utn.facturacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hace las cuentas de la factura. Es una clase "pura": no toca la base ni ARCA,
 * solo recibe numeros y devuelve numeros (por eso es facil de testear).
 *
 * Los precios de las listas son FINALES (IVA incluido). Entonces:
 *   - Factura C: neto = total, IVA = 0.
 *   - Factura A/B: por cada alicuota, neto = total / (1 + alicuota/100) y IVA = total - neto.
 */
public final class CalculadoraImportes {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculadoraImportes() {
    }

    /** Un renglon: cuanto suma y con que % de IVA. */
    public record Linea(BigDecimal subtotal, BigDecimal alicuotaIva) {
    }

    /** IVA agrupado por alicuota (ARCA lo pide asi en el nodo <Iva>). */
    public record IvaPorAlicuota(BigDecimal alicuota, BigDecimal baseImponible, BigDecimal importe) {
    }

    public record Totales(BigDecimal neto, BigDecimal iva, BigDecimal total, List<IvaPorAlicuota> ivas) {
    }

    public static BigDecimal subtotal(BigDecimal cantidad, BigDecimal precioUnitario) {
        return cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totales calcular(int tipoComprobante, List<Linea> lineas) {
        BigDecimal total = dosDecimales(lineas.stream()
                .map(Linea::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        if (!TipoComprobante.discriminaIva(tipoComprobante)) {
            return new Totales(total, dosDecimales(BigDecimal.ZERO), total, List.of());
        }

        // 1) Sumamos los subtotales de cada alicuota (TreeMap = ordenado: 10.5, 21, ...)
        Map<BigDecimal, BigDecimal> totalPorAlicuota = new TreeMap<>();
        for (Linea linea : lineas) {
            totalPorAlicuota.merge(dosDecimales(linea.alicuotaIva()), linea.subtotal(), BigDecimal::add);
        }

        // 2) A cada grupo le sacamos el IVA de adentro
        List<IvaPorAlicuota> ivas = new ArrayList<>();
        BigDecimal netoTotal = BigDecimal.ZERO;
        BigDecimal ivaTotal = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, BigDecimal> grupo : totalPorAlicuota.entrySet()) {
            BigDecimal alicuota = grupo.getKey();
            BigDecimal conIva = dosDecimales(grupo.getValue());
            BigDecimal divisor = BigDecimal.ONE.add(alicuota.divide(CIEN, 6, RoundingMode.HALF_UP));
            BigDecimal neto = conIva.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal iva = conIva.subtract(neto);

            ivas.add(new IvaPorAlicuota(alicuota, neto, iva));
            netoTotal = netoTotal.add(neto);
            ivaTotal = ivaTotal.add(iva);
        }
        return new Totales(dosDecimales(netoTotal), dosDecimales(ivaTotal), total, ivas);
    }

    private static BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
