package ar.edu.utn.facturacion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    /** Porcentaje de IVA: 21, 10.5, 27... */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getAlicuotaIva() { return alicuotaIva; }
    public void setAlicuotaIva(BigDecimal alicuotaIva) { this.alicuotaIva = alicuotaIva; }
}
