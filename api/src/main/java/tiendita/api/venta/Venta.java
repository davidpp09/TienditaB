package tiendita.api.venta;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    /** Costo de lo vendido. Guardado aquí para no recalcularlo en cada reporte. */
    @Column(name = "costo_total", nullable = false)
    private BigDecimal costoTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago;

    private BigDecimal recibido;

    private BigDecimal cambio;

    /** Una venta cancelada NO se borra: se marca y se revierte con movimientos. */
    @Column(nullable = false)
    private boolean cancelada = false;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(nullable = false, length = 40)
    private String usuario;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    protected Venta() {}

    public Venta(FormaPago formaPago, BigDecimal recibido, String usuario) {
        this.formaPago = formaPago;
        this.recibido = recibido;
        this.usuario = usuario;
    }

    /** El folio del ticket se deriva del id. No hay columna que mantener. */
    public String getFolio() {
        return id == null ? "" : String.format("V%06d", id);
    }

    public void agregar(VentaDetalle detalle) {
        detalles.add(detalle);
    }

    public void totalizar(BigDecimal total, BigDecimal costoTotal) {
        this.total = total;
        this.costoTotal = costoTotal;
        if (recibido != null && formaPago.mueveElCajon()) {
            this.cambio = recibido.subtract(total);
        }
    }

    public void cancelar(String motivo) {
        this.cancelada = true;
        this.canceladaEn = LocalDateTime.now();
        this.motivoCancelacion = motivo;
    }

    /** Utilidad de esta venta: lo que quedó después del costo de lo vendido. */
    public BigDecimal getUtilidad() {
        return total.subtract(costoTotal);
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getCostoTotal() { return costoTotal; }
    public FormaPago getFormaPago() { return formaPago; }
    public BigDecimal getRecibido() { return recibido; }
    public BigDecimal getCambio() { return cambio; }
    public boolean isCancelada() { return cancelada; }
    public LocalDateTime getCanceladaEn() { return canceladaEn; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public String getUsuario() { return usuario; }
    public List<VentaDetalle> getDetalles() { return detalles; }
}
