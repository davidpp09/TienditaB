package tiendita.api.proveedor;

import jakarta.persistence.*;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    /** Lo que tarda en surtir. Entra en la fórmula de la sugerencia de compra. */
    @Column(name = "dias_entrega", nullable = false)
    private int diasEntrega = 7;

    private String notas;

    @Column(nullable = false)
    private boolean activo = true;

    protected Proveedor() {}

    public Proveedor(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public int getDiasEntrega() { return diasEntrega; }
    public String getNotas() { return notas; }
    public boolean isActivo() { return activo; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setDiasEntrega(int diasEntrega) { this.diasEntrega = diasEntrega; }
    public void setNotas(String notas) { this.notas = notas; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
