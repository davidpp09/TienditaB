package tiendita.api.compra;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras")
public class CompraController {

    private final CompraService servicio;

    public CompraController(CompraService servicio) {
        this.servicio = servicio;
    }

    /**
     * Registra la compra y devuelve, junto con ella, los avisos de los productos
     * que subieron de costo. Van en la misma respuesta a propósito: es el único
     * momento en que David tiene la factura en la mano y puede decidir el precio
     * nuevo. Un correo al día siguiente ya llega tarde.
     */
    @PostMapping
    public CompraDTO.Vista registrar(@RequestBody @Valid CompraDTO.Registro datos) {
        return servicio.registrar(datos, usuario());
    }

    /**
     * Devolverle mercancía al proveedor. Va colgado de la compra y no suelto:
     * el costo al que salen esas piezas es el que se pagó por ellas, y eso solo
     * se sabe sabiendo de qué compra vinieron.
     */
    @PostMapping("/{id}/devolucion")
    public CompraDTO.DevolucionVista devolver(@PathVariable Long id,
                                              @RequestBody @Valid CompraDTO.Devolucion datos) {
        return servicio.devolver(id, datos, usuario());
    }

    @GetMapping
    public List<CompraDTO.Vista> ultimas(@RequestParam(required = false) Long proveedorId) {
        return proveedorId == null ? servicio.ultimas() : servicio.deProveedor(proveedorId);
    }

    @GetMapping("/{id}")
    public CompraDTO.Vista porId(@PathVariable Long id) {
        return servicio.vista(id);
    }

    /** "¿Quién me vende más barato?" y "¿desde cuándo me está subiendo este?". */
    @GetMapping("/historial/{productoId}")
    public List<CompraDTO.PrecioPagado> historial(@PathVariable Long productoId) {
        return servicio.historialDe(productoId);
    }

    /** Todavía no hay login: todo se firma como "mostrador". */
    private String usuario() {
        return "mostrador";
    }
}
