package tiendita.api.venta;

import tiendita.api.producto.Producto;
import tiendita.api.producto.Unidad;

import java.math.BigDecimal;

/** Ventas armadas a mano, sin base de datos: el ticket es formato puro. */
final class VentaDePrueba {

    private VentaDePrueba() {}

    static Venta mixta() {
        Venta venta = new Venta(FormaPago.EFECTIVO, new BigDecimal("100.00"), "test");
        venta.agregar(new VentaDetalle(venta,
                new Producto("750100", "Coca 600ml", Unidad.PIEZA, new BigDecimal("18.00")),
                new BigDecimal("2"), new BigDecimal("18.00"), new BigDecimal("13.5000"), new BigDecimal("36.00")));
        venta.agregar(new VentaDetalle(venta,
                new Producto(null, "Frijol a granel", Unidad.KILO, new BigDecimal("32.00")),
                new BigDecimal("1.250"), new BigDecimal("32.00"), new BigDecimal("24.0000"), new BigDecimal("40.00")));
        venta.totalizar(new BigDecimal("76.00"), new BigDecimal("57.00"));
        return venta;
    }

    static Venta conNombreLargo() {
        Venta venta = new Venta(FormaPago.EFECTIVO, new BigDecimal("2000.00"), "test");
        venta.agregar(new VentaDetalle(venta,
                new Producto(null, "Paquete de vasos desechables del número 10 con tapa transparente",
                        Unidad.PIEZA, new BigDecimal("617.00")),
                new BigDecimal("2"), new BigDecimal("617.00"), new BigDecimal("500.0000"), new BigDecimal("1234.00")));
        venta.totalizar(new BigDecimal("1234.00"), new BigDecimal("1000.00"));
        return venta;
    }
}
