package tiendita.api.producto;

import jakarta.persistence.*;
import tiendita.api.categoria.Categoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL en el granel y en lo que no trae código impreso. */
    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Unidad unidad = Unidad.PIEZA;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

    @Column(name = "precio_mayoreo")
    private BigDecimal precioMayoreo;

    @Column(name = "costo_promedio", nullable = false)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    /** Caché del kardex. La verdad son los movimientos, no este campo. */
    @Column(nullable = false)
    private BigDecimal existencia = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "dias_proveedor", nullable = false)
    private int diasProveedor = 7;

    /** Borrado suave, igual que en RestFood: un producto con ventas no se borra. */
    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Producto() {}

    public Producto(String codigoBarras, String nombre, Unidad unidad, BigDecimal precioVenta) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.unidad = unidad;
        this.precioVenta = precioVenta;
    }

    public Long getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public Categoria getCategoria() { return categoria; }
    public Unidad getUnidad() { return unidad; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public BigDecimal getPrecioMayoreo() { return precioMayoreo; }
    public BigDecimal getCostoPromedio() { return costoPromedio; }
    public BigDecimal getExistencia() { return existencia; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public int getDiasProveedor() { return diasProveedor; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setUnidad(Unidad unidad) { this.unidad = unidad; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public void setPrecioMayoreo(BigDecimal precioMayoreo) { this.precioMayoreo = precioMayoreo; }
    public void setCostoPromedio(BigDecimal costoPromedio) { this.costoPromedio = costoPromedio; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }
    public void setDiasProveedor(int diasProveedor) { this.diasProveedor = diasProveedor; }
    public void setActivo(boolean activo) { this.activo = activo; }

    /**
     * Mueve la existencia. Se permite quedar en negativo a propósito: si el
     * conteo está mal, la venta NO se puede detener — se cobra y el negativo
     * queda a la vista para corregirlo en el siguiente ajuste.
     */
    public void moverExistencia(BigDecimal cantidadConSigno) {
        this.existencia = this.existencia.add(cantidadConSigno);
    }
}
