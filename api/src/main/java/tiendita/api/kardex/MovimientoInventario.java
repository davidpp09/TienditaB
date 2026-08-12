package tiendita.api.kardex;

import jakarta.persistence.*;
import tiendita.api.producto.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un renglón del kardex. Esta tabla solo crece: no se edita ni se borra nada.
 * Cancelar una venta no borra su movimiento, agrega el contrario.
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    /** Con signo: entradas positivas, salidas negativas. */
    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    /** Foto de la existencia justo después de aplicar este movimiento. */
    @Column(name = "existencia_resultante", nullable = false)
    private BigDecimal existenciaResultante;

    @Column(name = "referencia_tipo", length = 20)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    private String motivo;

    @Column(nullable = false, length = 40)
    private String usuario;

    protected MovimientoInventario() {}

    public MovimientoInventario(Producto producto, TipoMovimiento tipo, BigDecimal cantidad,
                                BigDecimal costoUnitario, BigDecimal existenciaResultante,
                                String referenciaTipo, Long referenciaId, String motivo, String usuario) {
        this.producto = producto;
        this.fechaHora = LocalDateTime.now();
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.existenciaResultante = existenciaResultante;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.motivo = motivo;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public TipoMovimiento getTipo() { return tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public BigDecimal getExistenciaResultante() { return existenciaResultante; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public Long getReferenciaId() { return referenciaId; }
    public String getMotivo() { return motivo; }
    public String getUsuario() { return usuario; }
}
