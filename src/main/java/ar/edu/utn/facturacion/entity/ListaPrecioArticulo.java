package ar.edu.utn.facturacion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** El precio de UN articulo dentro de UNA lista de precios. */
@Entity
public class ListaPrecioArticulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ListaPrecio listaPrecio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Articulo articulo;

    /** Precio final, IVA incluido. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ListaPrecio getListaPrecio() { return listaPrecio; }
    public void setListaPrecio(ListaPrecio listaPrecio) { this.listaPrecio = listaPrecio; }
    public Articulo getArticulo() { return articulo; }
    public void setArticulo(Articulo articulo) { this.articulo = articulo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
