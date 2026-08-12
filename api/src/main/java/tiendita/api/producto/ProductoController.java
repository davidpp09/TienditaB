package tiendita.api.producto;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiendita.api.categoria.CategoriaRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService servicio;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ProductoController(ProductoService servicio, ProductoRepository productos,
                              CategoriaRepository categorias) {
        this.servicio = servicio;
        this.productos = productos;
        this.categorias = categorias;
    }

    /**
     * Lo que llama el lector de código de barras. Devuelve 404 si no existe, y
     * la pantalla usa ese 404 para abrir el alta rápida en lugar de dar error.
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ProductoDTO.Vista> porCodigo(@PathVariable String codigo) {
        return servicio.porCodigo(codigo)
                .map(ProductoDTO.Vista::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ProductoDTO.Vista> buscar(@RequestParam(required = false) String q) {
        return servicio.buscar(q).stream().map(ProductoDTO.Vista::de).toList();
    }

    @GetMapping("/{id}")
    public ProductoDTO.Vista porId(@PathVariable Long id) {
        return ProductoDTO.Vista.de(servicio.porId(id));
    }

    @PostMapping
    public ProductoDTO.Vista altaRapida(@RequestBody @Valid ProductoDTO.AltaRapida datos) {
        return ProductoDTO.Vista.de(servicio.altaRapida(datos));
    }

    @PutMapping("/{id}")
    public ProductoDTO.Vista editar(@PathVariable Long id, @RequestBody @Valid ProductoDTO.Edicion datos) {
        return ProductoDTO.Vista.de(servicio.editar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/merma")
    public ProductoDTO.Vista merma(@PathVariable Long id, @RequestBody @Valid ProductoDTO.Ajuste ajuste) {
        return ProductoDTO.Vista.de(servicio.merma(id, ajuste.cantidad(), ajuste.motivo(), usuario()));
    }

    @PostMapping("/{id}/autoconsumo")
    public ProductoDTO.Vista autoconsumo(@PathVariable Long id, @RequestBody @Valid ProductoDTO.Ajuste ajuste) {
        return ProductoDTO.Vista.de(servicio.autoconsumo(id, ajuste.cantidad(), ajuste.motivo(), usuario()));
    }

    /** El cuerpo lleva la existencia REAL contada, no la diferencia. */
    @PostMapping("/{id}/conteo")
    public ProductoDTO.Vista conteo(@PathVariable Long id, @RequestBody @Valid ProductoDTO.Ajuste ajuste) {
        return ProductoDTO.Vista.de(servicio.ajustarPorConteo(id, ajuste.cantidad(), ajuste.motivo(), usuario()));
    }

    @GetMapping("/stock-bajo")
    public List<ProductoDTO.Vista> stockBajo() {
        return productos.conStockBajo().stream().map(ProductoDTO.Vista::de).toList();
    }

    @GetMapping("/categorias")
    public List<Map<String, Object>> categorias() {
        return categorias.findByActivoTrueOrderByNombre().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "nombre", c.getNombre()))
                .toList();
    }

    /**
     * Todavía no hay login (llega en el siguiente PR). Hasta entonces todo queda
     * firmado como "mostrador", que es la verdad: hay una sola caja.
     */
    private String usuario() {
        return "mostrador";
    }
}
