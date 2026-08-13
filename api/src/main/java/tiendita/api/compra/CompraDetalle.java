package tiendita.api.compra;

import jakarta.persistence.*;
import tiendita.api.producto.Producto;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_detalle")
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private BigDecimal cantidad;

    /**
     * Lo que costó ESTA vez, con 4 decimales. No es el costo promedio del
     * producto: es el dato crudo del que sale el promedio, y el que contesta
     * "¿quién me vende más barato?" meses después.
     */
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private BigDecimal importe;

    protected CompraDetalle() {}

    public CompraDetalle(Compra compra, Producto producto, BigDecimal cantidad,
                         BigDecimal costoUnitario, BigDecimal importe) {
        this.compra = compra;
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.importe = importe;
    }

    public Long getId() { return id; }
    public Compra getCompra() { return compra; }
    public Producto getProducto() { return producto; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public BigDecimal getImporte() { return importe; }
}
