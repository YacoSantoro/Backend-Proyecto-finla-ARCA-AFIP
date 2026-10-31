package ar.edu.utn.facturacion.entity;

import jakarta.persistence.*;

@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String razonSocial;

    /** Codigo ARCA: 80 CUIT, 96 DNI, 99 Sin identificar. */
    private int docTipo;

    private long docNro;

    /** Codigo ARCA: 1 Resp. Inscripto, 4 Exento, 5 Consumidor Final, 6 Monotributo. */
    private int condicionIva;

    private String direccion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public int getDocTipo() { return docTipo; }
    public void setDocTipo(int docTipo) { this.docTipo = docTipo; }
    public long getDocNro() { return docNro; }
    public void setDocNro(long docNro) { this.docNro = docNro; }
    public int getCondicionIva() { return condicionIva; }
    public void setCondicionIva(int condicionIva) { this.condicionIva = condicionIva; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
