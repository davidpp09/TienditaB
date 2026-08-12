package tiendita.api.venta;

import jakarta.persistence.*;
import tiendita.api.producto.Producto;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_detalle")
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    /** Copia del nombre: si el producto se renombra, este ticket no cambia. */
    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    /**
     * El costo del momento en que se vendió. Es lo que hace que el margen de
     * esta venta siga siendo cierto dentro de un año, aunque el costo del
     * producto haya cambiado seis veces.
     */
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private BigDecimal importe;

    protected VentaDetalle() {}

    public VentaDetalle(Venta venta, Producto producto, BigDecimal cantidad,
                        BigDecimal precioUnitario, BigDecimal costoUnitario, BigDecimal importe) {
        this.venta = venta;
        this.producto = producto;
        this.descripcion = producto.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.costoUnitario = costoUnitario;
        this.importe = importe;
    }

    public Long getId() { return id; }
    public Venta getVenta() { return venta; }
    public Producto getProducto() { return producto; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public BigDecimal getImporte() { return importe; }
}
