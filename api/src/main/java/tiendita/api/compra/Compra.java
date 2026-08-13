package tiendita.api.compra;

import jakarta.persistence.*;
import tiendita.api.comun.FormaPago;
import tiendita.api.proveedor.Proveedor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Una entrada de mercancía. Es el único evento que cambia el costo de un
 * producto: la venta lo copia, la merma lo usa, pero solo la compra lo mueve.
 */
@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /**
     * La fecha la pone quien captura, no el reloj: el proveedor llegó el martes
     * y la factura se captura el jueves, y el costo tiene que quedar fechado
     * cuando entró la mercancía.
     */
    @Column(nullable = false)
    private LocalDate fecha;

    /** El folio de la factura o de la nota del proveedor. Puede no traer. */
    private String folio;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    /** Falso cuando se queda a deber: no sale dinero de la caja. */
    @Column(nullable = false)
    private boolean pagada = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago = FormaPago.EFECTIVO;

    @Column(nullable = false, length = 40)
    private String usuario;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraDetalle> detalles = new ArrayList<>();

    protected Compra() {}

    public Compra(Proveedor proveedor, LocalDate fecha, String folio, boolean pagada,
                  FormaPago formaPago, String usuario) {
        this.proveedor = proveedor;
        this.fecha = fecha;
        this.folio = folio;
        this.pagada = pagada;
        this.formaPago = formaPago;
        this.usuario = usuario;
    }

    public void agregar(CompraDetalle detalle) {
        detalles.add(detalle);
    }

    public void totalizar(BigDecimal total) {
        this.total = total;
    }

    /**
     * Solo el dinero que salió del cajón cuenta para el corte. Lo que se pagó
     * por transferencia, o lo que se quedó a deber, no estuvo ahí.
     */
    public boolean salioDelCajon() {
        return pagada && formaPago.mueveElCajon();
    }

    public Long getId() { return id; }
    public Proveedor getProveedor() { return proveedor; }
    public LocalDate getFecha() { return fecha; }
    public String getFolio() { return folio; }
    public BigDecimal getTotal() { return total; }
    public boolean isPagada() { return pagada; }
    public FormaPago getFormaPago() { return formaPago; }
    public String getUsuario() { return usuario; }
    public List<CompraDetalle> getDetalles() { return detalles; }
}
