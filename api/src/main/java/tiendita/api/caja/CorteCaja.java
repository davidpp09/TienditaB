package tiendita.api.caja;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corte_caja")
public class CorteCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fondo_inicial", nullable = false)
    private BigDecimal fondoInicial;

    @Column(name = "total_ventas", nullable = false)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_gastos", nullable = false)
    private BigDecimal totalGastos = BigDecimal.ZERO;

    @Column(name = "total_retiros", nullable = false)
    private BigDecimal totalRetiros = BigDecimal.ZERO;

    /** Lo que debería haber en el cajón: fondo + ventas en efectivo − gastos − retiros. */
    @Column(nullable = false)
    private BigDecimal esperado = BigDecimal.ZERO;

    /** Lo que David contó de verdad. */
    @Column(nullable = false)
    private BigDecimal contado = BigDecimal.ZERO;

    /** contado − esperado. Se guarda aunque sea negativa. */
    @Column(nullable = false)
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Column(name = "abierto_en", nullable = false)
    private LocalDateTime abiertoEn = LocalDateTime.now();

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @Column(nullable = false, length = 40)
    private String usuario;

    private String notas;

    protected CorteCaja() {}

    public CorteCaja(BigDecimal fondoInicial, String usuario) {
        this.fecha = LocalDate.now();
        this.fondoInicial = fondoInicial;
        this.usuario = usuario;
    }

    public boolean estaAbierto() {
        return cerradoEn == null;
    }

    /**
     * Pone el fondo en una caja que se abrió sola. Pasa todos los días: la
     * primera venta entra antes de que nadie declare con cuánto empezó el cajón.
     */
    public void declararFondo(BigDecimal fondoInicial) {
        this.fondoInicial = fondoInicial;
    }

    public void cerrar(BigDecimal totalVentas, BigDecimal totalGastos, BigDecimal totalRetiros,
                       BigDecimal esperado, BigDecimal contado, String notas) {
        this.totalVentas = totalVentas;
        this.totalGastos = totalGastos;
        this.totalRetiros = totalRetiros;
        this.esperado = esperado;
        this.contado = contado;
        this.diferencia = contado.subtract(esperado);
        this.notas = notas;
        this.cerradoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getFondoInicial() { return fondoInicial; }
    public BigDecimal getTotalVentas() { return totalVentas; }
    public BigDecimal getTotalGastos() { return totalGastos; }
    public BigDecimal getTotalRetiros() { return totalRetiros; }
    public BigDecimal getEsperado() { return esperado; }
    public BigDecimal getContado() { return contado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public LocalDateTime getAbiertoEn() { return abiertoEn; }
    public LocalDateTime getCerradoEn() { return cerradoEn; }
    public String getUsuario() { return usuario; }
    public String getNotas() { return notas; }
}
