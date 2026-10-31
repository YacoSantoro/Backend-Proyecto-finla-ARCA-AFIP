package ar.edu.utn.facturacion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Un renglon de la factura. Guarda una COPIA del precio para que no cambie si cambia la lista. */
@Entity
public class FacturaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Articulo articulo;

    private String descripcion;

    @Column(precision = 15, scale = 3)
    private BigDecimal cantidad;

    /** Precio final (IVA incluido) copiado de la lista al momento de facturar. */
    @Column(precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }
    public Articulo getArticulo() { return articulo; }
    public void setArticulo(Articulo articulo) { this.articulo = articulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getAlicuotaIva() { return alicuotaIva; }
    public void setAlicuotaIva(BigDecimal alicuotaIva) { this.alicuotaIva = alicuotaIva; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
