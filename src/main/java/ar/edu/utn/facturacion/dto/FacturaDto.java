package ar.edu.utn.facturacion.dto;

import ar.edu.utn.facturacion.entity.Cliente;
import ar.edu.utn.facturacion.entity.Factura;
import ar.edu.utn.facturacion.entity.FacturaDetalle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** La factura completa, para la pantalla de edicion / detalle. */
public record FacturaDto(
        Long id,
        int puntoVenta,
        int tipoComprobante,
        Long numero,
        LocalDate fecha,
        Cliente cliente,
        Long listaPrecioId,
        String listaPrecioNombre,
        List<Item> items,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeTotal,
        boolean informadaArca,
        String cae,
        LocalDate caeVencimiento,
        LocalDateTime fechaInforme,
        String observacionesArca) {

    public record Item(Long articuloId, String codigo, String descripcion, BigDecimal cantidad,
                       BigDecimal precioUnitario, BigDecimal alicuotaIva, BigDecimal subtotal) {

        static Item de(FacturaDetalle d) {
            return new Item(d.getArticulo().getId(), d.getArticulo().getCodigo(), d.getDescripcion(),
                    d.getCantidad(), d.getPrecioUnitario(), d.getAlicuotaIva(), d.getSubtotal());
        }
    }

    public static FacturaDto de(Factura f) {
        // Copiamos el cliente a un objeto "suelto" para que Jackson no toque el proxy de Hibernate
        Cliente c = new Cliente();
        c.setId(f.getCliente().getId());
        c.setRazonSocial(f.getCliente().getRazonSocial());
        c.setDocTipo(f.getCliente().getDocTipo());
        c.setDocNro(f.getCliente().getDocNro());
        c.setCondicionIva(f.getCliente().getCondicionIva());
        c.setDireccion(f.getCliente().getDireccion());

        return new FacturaDto(f.getId(), f.getPuntoVenta(), f.getTipoComprobante(), f.getNumero(), f.getFecha(), c,
                f.getListaPrecio().getId(), f.getListaPrecio().getNombre(),
                f.getDetalles().stream().map(Item::de).toList(),
                f.getImporteNeto(), f.getImporteIva(), f.getImporteTotal(),
                f.isInformadaArca(), f.getCae(), f.getCaeVencimiento(), f.getFechaInforme(), f.getObservacionesArca());
    }
}
