package ar.edu.utn.facturacion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int puntoVenta;

    /** Codigo ARCA: 1 Factura A, 6 Factura B, 11 Factura C. */
    private int tipoComprobante;

    /** Lo asigna ARCA al informar. Mientras no se informa queda en null. */
    private Long numero;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ListaPrecio listaPrecio;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<FacturaDetalle> detalles = new ArrayList<>();

    @Column(precision = 15, scale = 2)
    private BigDecimal importeNeto = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal importeTotal = BigDecimal.ZERO;

    /** true = ya tiene CAE. A partir de ahi NO se puede modificar ni borrar. */
    private boolean informadaArca;

    private String cae;
    private LocalDate caeVencimiento;
    private LocalDateTime fechaInforme;

    @Column(length = 2000)
    private String observacionesArca;

    /** Agrega un renglon y le avisa al renglon a que factura pertenece. */
    public void agregarDetalle(FacturaDetalle detalle) {
        detalle.setFactura(this);
        detalles.add(detalle);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }
    public int getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(int tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public Long getNumero() { return numero; }
    public void setNumero(Long numero) { this.numero = numero; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public ListaPrecio getListaPrecio() { return listaPrecio; }
    public void setListaPrecio(ListaPrecio listaPrecio) { this.listaPrecio = listaPrecio; }
    public List<FacturaDetalle> getDetalles() { return detalles; }
    public BigDecimal getImporteNeto() { return importeNeto; }
    public void setImporteNeto(BigDecimal importeNeto) { this.importeNeto = importeNeto; }
    public BigDecimal getImporteIva() { return importeIva; }
    public void setImporteIva(BigDecimal importeIva) { this.importeIva = importeIva; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public void setImporteTotal(BigDecimal importeTotal) { this.importeTotal = importeTotal; }
    public boolean isInformadaArca() { return informadaArca; }
    public void setInformadaArca(boolean informadaArca) { this.informadaArca = informadaArca; }
    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }
    public LocalDate getCaeVencimiento() { return caeVencimiento; }
    public void setCaeVencimiento(LocalDate caeVencimiento) { this.caeVencimiento = caeVencimiento; }
    public LocalDateTime getFechaInforme() { return fechaInforme; }
    public void setFechaInforme(LocalDateTime fechaInforme) { this.fechaInforme = fechaInforme; }
    public String getObservacionesArca() { return observacionesArca; }
    public void setObservacionesArca(String observacionesArca) { this.observacionesArca = observacionesArca; }
}
