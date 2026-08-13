package tiendita.api.compra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.caja.CajaService;
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

/** Regresarle mercancía al proveedor: llegó caduca, rota, o de más. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DevolucionTest {

    @Autowired CompraService compras;
    @Autowired ProductoRepository productos;
    @Autowired ProveedorRepository proveedores;
    @Autowired MovimientoInventarioRepository kardex;
    @Autowired CajaService caja;

    private Proveedor abarrotera;

    private Producto producto(String nombre, Unidad unidad, String precio, String costo, String existencia) {
        Producto p = new Producto(null, nombre, unidad, new BigDecimal(precio));
        p.setCostoPromedio(new BigDecimal(costo));
        p.moverExistencia(new BigDecimal(existencia));
        return productos.save(p);
    }

    private Long comprar(Producto p, String cantidad, String costo, boolean pagada, FormaPago formaPago) {
        if (abarrotera == null) {
            abarrotera = proveedores.save(new Proveedor("Abarrotera del Centro"));
        }
        return compras.registrar(new CompraDTO.Registro(abarrotera.getId(), LocalDate.now(), "F-100",
                pagada, formaPago,
                List.of(new CompraDTO.Linea(p.getId(), new BigDecimal(cantidad), new BigDecimal(costo)))),
                "test").id();
    }

    private Long comprar(Producto p, String cantidad, String costo) {
        return comprar(p, cantidad, costo, true, FormaPago.EFECTIVO);
    }

    private CompraDTO.DevolucionVista devolver(Long compraId, Producto p, String cantidad, String motivo) {
        return compras.devolver(compraId,
                new CompraDTO.Devolucion(motivo,
                        List.of(new CompraDTO.LineaDevuelta(p.getId(), new BigDecimal(cantidad)))),
                "test");
    }

    @Test
    void laDevolucionSacaDelInventarioYEscribeElKardex() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long compra = comprar(atun, "20", "20.60");

        CompraDTO.DevolucionVista devolucion = devolver(compra, atun, "5", "llegaron abolladas");

        assertThat(atun.getExistencia()).isEqualByComparingTo("15");
        assertThat(devolucion.total()).isEqualByComparingTo("103.00");
        assertThat(devolucion.lineas().get(0).costoUnitario()).isEqualByComparingTo("20.60");

        List<MovimientoInventario> movs = kardex.findByProductoIdOrderByFechaHoraDescIdDesc(atun.getId());
        assertThat(movs.get(0).getTipo()).isEqualTo(TipoMovimiento.DEVOLUCION_PROVEEDOR);
        assertThat(movs.get(0).getCantidad()).isEqualByComparingTo("-5");
        assertThat(movs.get(0).getCostoUnitario()).isEqualByComparingTo("20.60");
        assertThat(movs.get(0).getMotivo()).isEqualTo("llegaron abolladas");
        assertThat(movs.get(0).getReferenciaId()).isEqualTo(compra);
    }

    /**
     * El corazón de la devolución: sale al costo que se PAGÓ, no al costo
     * promedio de hoy. Se tenían 10 piezas a $18.00, entraron 20 a $20.60
     * dejando el promedio en $19.7333, y se devuelve el lote caro completo: el
     * promedio vuelve a donde estaba.
     *
     * <p>Vuelve a $17.9999 y no a $18.0000 exacto porque el promedio se guarda
     * con 4 decimales; es una diezmilésima de peso por pieza. Lo explica
     * CostoPromedioTest.
     */
    @Test
    void devolverElLoteCaroDevuelveElCostoPromedioADondeEstaba() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "18.0000", "10");
        Long compra = comprar(atun, "20", "20.60");
        assertThat(atun.getCostoPromedio()).isEqualByComparingTo("19.7333");

        devolver(compra, atun, "20", "el lote venía caduco");

        assertThat(atun.getExistencia()).isEqualByComparingTo("10");
        assertThat(atun.getCostoPromedio()).isEqualByComparingTo("17.9999");
    }

    @Test
    void elKardexSigueCuadrandoDespuesDeDevolver() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long compra = comprar(atun, "20", "20.60");

        devolver(compra, atun, "8", "sobraron");

        assertThat(kardex.sumaCantidades(atun.getId())).isEqualByComparingTo(atun.getExistencia());
        assertThat(atun.getExistencia()).isEqualByComparingTo("12");
    }

    @Test
    void elDineroRegresaAlCajonSiDeAhiHabiaSalido() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long corte = caja.corteAbierto("test").getId();
        Long compra = comprar(atun, "20", "20.60");
        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("-412.00");

        CompraDTO.DevolucionVista devolucion = devolver(compra, atun, "20", "caduco");

        assertThat(devolucion.regresoAlCajon()).isTrue();
        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("0.00");
    }

    /**
     * Si se pagó por transferencia, el dinero nunca estuvo en el cajón: lo que
     * cambia con la devolución es lo que se le debe al proveedor, no el
     * efectivo. Meterlo al cajón inventaría un sobrante en el corte.
     */
    @Test
    void loQueNoSalioDelCajonNoRegresaAlCajon() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long corte = caja.corteAbierto("test").getId();
        Long compra = comprar(atun, "20", "20.60", true, FormaPago.TRANSFERENCIA);

        CompraDTO.DevolucionVista devolucion = devolver(compra, atun, "20", "caduco");

        assertThat(devolucion.regresoAlCajon()).isFalse();
        assertThat(caja.esperadoEnCaja(corte)).isEqualByComparingTo("0.00");
        assertThat(atun.getExistencia()).isEqualByComparingTo("0");   // la mercancía sí salió
    }

    @Test
    void noSePuedeDevolverMasDeLoQueTraiaLaCompra() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long compra = comprar(atun, "20", "20.60");

        assertThatThrownBy(() -> devolver(compra, atun, "25", "caduco"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("solo quedan 20 por devolver");
    }

    /** Sin esto se podría devolver el mismo lote dos veces y cobrarlo dos veces. */
    @Test
    void lasDevolucionesAnterioresCuentanContraElTope() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long compra = comprar(atun, "20", "20.60");
        devolver(compra, atun, "15", "caduco");

        assertThatThrownBy(() -> devolver(compra, atun, "10", "más caduco"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("solo quedan 5 por devolver");

        devolver(compra, atun, "5", "el resto");
        assertThat(atun.getExistencia()).isEqualByComparingTo("0");
    }

    @Test
    void noSePuedeDevolverAlgoQueEsaCompraNoTrajo() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Producto vasos = producto("Vasos #10", Unidad.PIEZA, "45.00", "0", "50");
        Long compra = comprar(atun, "20", "20.60");

        assertThatThrownBy(() -> devolver(compra, vasos, "5", "no son míos"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no trae el producto");
    }

    /** Igual que la merma y el ajuste: sin motivo no se mueve el inventario. */
    @Test
    void exigeDecirPorQueSeDevuelve() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Long compra = comprar(atun, "20", "20.60");

        assertThatThrownBy(() -> devolver(compra, atun, "5", "   "))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("por qué se devuelve");
    }

    @Test
    void elGranelSeDevuelveConDecimales() {
        Producto frijol = producto("Frijol a granel", Unidad.KILO, "32.00", "0", "0");
        Long compra = comprar(frijol, "12.500", "24.80");

        CompraDTO.DevolucionVista devolucion = devolver(compra, frijol, "2.500", "venía con gorgojo");

        assertThat(frijol.getExistencia()).isEqualByComparingTo("10.000");
        assertThat(devolucion.total()).isEqualByComparingTo("62.00");
    }

    @Test
    void unaPiezaNoSeDevuelveEnFracciones() {
        Producto coca = producto("Coca-Cola 600 ml", Unidad.PIEZA, "18.00", "0", "0");
        Long compra = comprar(coca, "24", "13.50");

        assertThatThrownBy(() -> devolver(compra, coca, "1.5", "rotas"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("la cantidad debe ser entera");
    }

    @Test
    void seDevuelvenVariosProductosDeUnaSolaVez() {
        Producto atun = producto("Atún en agua", Unidad.PIEZA, "21.00", "0", "0");
        Producto frijol = producto("Frijol a granel", Unidad.KILO, "32.00", "0", "0");
        abarrotera = proveedores.save(new Proveedor("Abarrotera del Centro"));
        Long compra = compras.registrar(new CompraDTO.Registro(abarrotera.getId(), LocalDate.now(), "F-300",
                true, FormaPago.EFECTIVO,
                List.of(new CompraDTO.Linea(atun.getId(), new BigDecimal("20"), new BigDecimal("20.60")),
                        new CompraDTO.Linea(frijol.getId(), new BigDecimal("12.500"), new BigDecimal("24.80")))),
                "test").id();

        CompraDTO.DevolucionVista devolucion = compras.devolver(compra,
                new CompraDTO.Devolucion("vino todo el pedido mal",
                        List.of(new CompraDTO.LineaDevuelta(atun.getId(), new BigDecimal("20")),
                                new CompraDTO.LineaDevuelta(frijol.getId(), new BigDecimal("12.500")))),
                "test");

        assertThat(devolucion.lineas()).hasSize(2);
        assertThat(devolucion.total()).isEqualByComparingTo("722.00");   // 412.00 + 310.00
        assertThat(atun.getExistencia()).isEqualByComparingTo("0");
        assertThat(frijol.getExistencia()).isEqualByComparingTo("0");
    }
}
