package tiendita.api.compra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
import tiendita.api.caja.MovimientoCaja;
import tiendita.api.caja.TipoMovimientoCaja;
import tiendita.api.comun.FormaPago;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.kardex.MovimientoInventario;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.kardex.TipoMovimiento;
import tiendita.api.producto.Producto;
import tiendita.api.producto.ProductoRepository;
import tiendita.api.producto.Unidad;
import tiendita.api.proveedor.Proveedor;
import tiendita.api.proveedor.ProveedorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompraTest {

    @Autowired CompraService compras;
    @Autowired ProductoRepository productos;
    @Autowired ProveedorRepository proveedores;
    @Autowired MovimientoInventarioRepository kardex;
    @Autowired CajaService caja;

    private Proveedor proveedor() {
        return proveedores.save(new Proveedor("Abarrotera del Centro"));
    }

    private Producto producto(String nombre, Unidad unidad, String precio, String costo, String existencia) {
        Producto p = new Producto(null, nombre, unidad, new BigDecimal(precio));
        p.setCostoPromedio(new BigDecimal(costo));
        p.moverExistencia(new BigDecimal(existencia));
        return productos.save(p);
    }

    private CompraDTO.Vista comprar(Proveedor proveedor, Producto p, String cantidad, String costo) {
        return comprar(proveedor, p, cantidad, costo, true, FormaPago.EFECTIVO);
    }

    private CompraDTO.Vista comprar(Proveedor proveedor, Producto p, String cantidad, String costo,
                                    boolean pagada, FormaPago formaPago) {
        return compras.registrar(new CompraDTO.Registro(proveedor.getId(), LocalDate.now(), "F-100",
                pagada, formaPago,
                List.of(new CompraDTO.Linea(p.getId(), new BigDecimal(cantidad), new BigDecimal(costo)))), "test");
    }

    @Test
    void laCompraEntraAlInventarioYRecalculaElCostoPromedio() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "10");

        CompraDTO.Vista compra = comprar(abarrotera, atun, "20", "20.60");

        assertThat(atun.getExistencia()).isEqualByComparingTo("30");
        assertThat(atun.getCostoPromedio()).isEqualByComparingTo("19.7333");
        assertThat(compra.total()).isEqualByComparingTo("412.00");
        assertThat(compra.lineas()).hasSize(1);
        assertThat(compra.lineas().get(0).costoPromedioResultante()).isEqualByComparingTo("19.7333");
    }

    /**
     * El kardex guarda lo que PASÓ: estas piezas costaron lo de la factura. El
     * promedio resultante es otra cosa y vive en el producto.
     */
    @Test
    void elKardexAnotaLaEntradaAlCostoQueSePago() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "10");

        CompraDTO.Vista compra = comprar(abarrotera, atun, "20", "20.60");

        List<MovimientoInventario> movs = kardex.findByProductoIdOrderByFechaHoraDescIdDesc(atun.getId());
        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).getTipo()).isEqualTo(TipoMovimiento.COMPRA);
        assertThat(movs.get(0).getCantidad()).isEqualByComparingTo("20");
        assertThat(movs.get(0).getCostoUnitario()).isEqualByComparingTo("20.60");
        assertThat(movs.get(0).getExistenciaResultante()).isEqualByComparingTo("30");
        assertThat(movs.get(0).getReferenciaId()).isEqualTo(compra.id());
    }

    /** El caché del producto tiene que seguir cuadrando con el libro. */
    @Test
    void elKardexSigueCuadrandoDespuesDeUnaCompra() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "0");

        comprar(abarrotera, atun, "20", "20.60");

        assertThat(kardex.sumaCantidades(atun.getId())).isEqualByComparingTo(atun.getExistencia());
    }

    @Test
    void elTotalLoSumaElServidorRenglonPorRenglon() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "0");
        Producto frijol = producto("Frijol a granel", Unidad.KILO, "32.00", "24.0000", "0");

        CompraDTO.Vista compra = compras.registrar(new CompraDTO.Registro(
                abarrotera.getId(), LocalDate.now(), "F-200", true, FormaPago.EFECTIVO,
                List.of(new CompraDTO.Linea(atun.getId(), new BigDecimal("20"), new BigDecimal("20.60")),
                        new CompraDTO.Linea(frijol.getId(), new BigDecimal("12.500"), new BigDecimal("24.80")))),
                "test");

        assertThat(compra.total()).isEqualByComparingTo("722.00");   // 412.00 + 310.00
    }

    @Test
    void laCompraPagadaEnEfectivoSaleDeLaCaja() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "0");
        Long corte = caja.corteAbierto("test").getId();

        comprar(abarrotera, atun, "20", "20.60");

        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("-412.00");
        List<MovimientoCaja> movimientos = caja.movimientosDelCorte(corte);
        assertThat(movimientos).anyMatch(m -> m.getTipo() == TipoMovimientoCaja.COMPRA);
        assertThat(movimientos.get(0).getConcepto()).contains("Abarrotera del Centro").contains("F-100");
    }

    /**
     * Lo pagado por transferencia nunca estuvo en el cajón. Si se descontara del
     * corte, la noche cerraría con un faltante inventado de cientos de pesos.
     * Es la razón de ser de la migración V2.
     */
    @Test
    void laCompraPagadaPorTransferenciaNoTocaElCajon() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "0");
        Long corte = caja.corteAbierto("test").getId();

        comprar(abarrotera, atun, "20", "20.60", true, FormaPago.TRANSFERENCIA);

        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("0.00");
    }

    /** Lo que se queda a deber tampoco: el dinero sigue en el cajón. */
    @Test
    void loQueSeQuedaADeberNoSaleDeLaCaja() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "0");
        Long corte = caja.corteAbierto("test").getId();

        CompraDTO.Vista compra = comprar(abarrotera, atun, "20", "20.60", false, FormaPago.EFECTIVO);

        assertThat(compra.salioDelCajon()).isFalse();
        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("0.00");
        assertThat(atun.getExistencia()).isEqualByComparingTo("20");   // la mercancía sí entró
    }

    @Test
    void laPrimeraCompraNoAvisaPeroLaSegundaSi() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");

        assertThat(comprar(abarrotera, atun, "20", "18.50").avisos()).isEmpty();

        List<AvisoDeCosto> avisos = comprar(abarrotera, atun, "20", "20.60").avisos();

        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).costoAnterior()).isEqualByComparingTo("18.50");
        assertThat(avisos.get(0).costoNuevo()).isEqualByComparingTo("20.60");
        assertThat(avisos.get(0).precioSugerido()).isEqualByComparingTo("23.38");
    }

    /**
     * El aviso compara contra lo último que se PAGÓ, no contra el promedio: aquí
     * el promedio quedó en 19.53 por las piezas viejas y baratas, pero lo que va
     * a costar reponer el atún mañana son los $20.60 de la factura.
     */
    @Test
    void elAvisoCompararContraElUltimoCostoPagadoNoContraElPromedio() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        comprar(abarrotera, atun, "20", "18.50");

        List<AvisoDeCosto> avisos = comprar(abarrotera, atun, "10", "20.60").avisos();

        assertThat(atun.getCostoPromedio()).isEqualByComparingTo("19.2000");
        assertThat(avisos.get(0).costoAnterior()).isEqualByComparingTo("18.50");
        assertThat(avisos.get(0).costoNuevo()).isEqualByComparingTo("20.60");
    }

    @Test
    void noAvisaCuandoElProveedorNoSubioElPrecio() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        comprar(abarrotera, atun, "20", "18.50");

        assertThat(comprar(abarrotera, atun, "20", "18.00").avisos()).isEmpty();
    }

    /** El aviso sugiere; el precio no se mueve solo. Subirlo lo decide David. */
    @Test
    void elAvisoNoCambiaElPrecioDeVenta() {
        Proveedor abarrotera = proveedor();
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        comprar(abarrotera, atun, "20", "18.50");

        comprar(abarrotera, atun, "20", "20.60");

        assertThat(atun.getPrecioVenta()).isEqualByComparingTo("21.00");
    }

    @Test
    void elHistorialDiceQuienVendioMasBaratoYCuando() {
        Proveedor abarrotera = proveedor();
        Proveedor otro = proveedores.save(new Proveedor("Mayoreo López"));
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");

        comprar(abarrotera, atun, "20", "18.50");
        comprar(otro, atun, "20", "17.90");

        List<CompraDTO.PrecioPagado> historial = compras.historialDe(atun.getId());

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).proveedor()).isEqualTo("Mayoreo López");
        assertThat(historial.get(0).costoUnitario()).isEqualByComparingTo("17.90");
        assertThat(historial.get(1).proveedor()).isEqualTo("Abarrotera del Centro");
    }

    @Test
    void elGranelSeCompraConDecimales() {
        Proveedor abarrotera = proveedor();
        Producto frijol = producto("Frijol a granel", Unidad.KILO, "32.00", "0", "0");

        CompraDTO.Vista compra = comprar(abarrotera, frijol, "12.500", "24.80");

        assertThat(frijol.getExistencia()).isEqualByComparingTo("12.500");
        assertThat(compra.total()).isEqualByComparingTo("310.00");
    }

    /** Una caja de 24 refrescos se captura como 24 piezas, no como 1 caja. */
    @Test
    void unaPiezaNoSeCompraEnFracciones() {
        Proveedor abarrotera = proveedor();
        Producto coca = producto("Coca-Cola 600 ml", Unidad.PIEZA, "18.00", "0", "0");

        assertThatThrownBy(() -> comprar(abarrotera, coca, "1.5", "13.50"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("por pieza");
    }

    @Test
    void noSeCompraCantidadCeroONegativa() {
        Proveedor abarrotera = proveedor();
        Producto coca = producto("Coca-Cola 600 ml", Unidad.PIEZA, "18.00", "0", "0");

        assertThatThrownBy(() -> comprar(abarrotera, coca, "0", "13.50"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("mayor que cero");
    }

    /**
     * El caso del arranque: el producto se dio de alta en el mostrador y se
     * vendió antes de comprarlo, así que quedó en negativo. La compra lo
     * levanta y adopta el costo de la factura sin inventar promedios.
     */
    @Test
    void laCompraLevantaUnProductoQueQuedoEnNegativo() {
        Proveedor abarrotera = proveedor();
        Producto vasos = producto("Vasos #10", Unidad.PIEZA, "45.00", "0", "-3");

        comprar(abarrotera, vasos, "50", "38.00");

        assertThat(vasos.getExistencia()).isEqualByComparingTo("47");
        assertThat(vasos.getCostoPromedio()).isEqualByComparingTo("38.0000");
    }
}
