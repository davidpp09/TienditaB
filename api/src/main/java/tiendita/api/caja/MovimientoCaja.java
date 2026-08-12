package tiendita.api.caja;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoCaja tipo;

    /** Con signo: entradas positivas, salidas negativas. */
    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String concepto;

    @Column(name = "categoria_gasto", length = 50)
    private String categoriaGasto;

    @Column(name = "referencia_tipo", length = 20)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_id")
    private CorteCaja corte;

    @Column(nullable = false, length = 40)
    private String usuario;

    protected MovimientoCaja() {}

    public MovimientoCaja(TipoMovimientoCaja tipo, BigDecimal monto, String concepto,
                          CorteCaja corte, String usuario) {
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.corte = corte;
        this.usuario = usuario;
    }

    public MovimientoCaja conReferencia(String tipo, Long id) {
        this.referenciaTipo = tipo;
        this.referenciaId = id;
        return this;
    }

    public MovimientoCaja conCategoriaGasto(String categoria) {
        this.categoriaGasto = categoria;
        return this;
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public TipoMovimientoCaja getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public String getConcepto() { return concepto; }
    public String getCategoriaGasto() { return categoriaGasto; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public Long getReferenciaId() { return referenciaId; }
    public CorteCaja getCorte() { return corte; }
    public String getUsuario() { return usuario; }
}
