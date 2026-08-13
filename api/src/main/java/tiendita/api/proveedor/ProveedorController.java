package tiendita.api.proveedor;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService servicio;

    public ProveedorController(ProveedorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ProveedorDTO.Vista> buscar(@RequestParam(required = false) String q) {
        return servicio.buscar(q).stream().map(ProveedorDTO.Vista::de).toList();
    }

    @GetMapping("/{id}")
    public ProveedorDTO.Vista porId(@PathVariable Long id) {
        return ProveedorDTO.Vista.de(servicio.porId(id));
    }

    @PostMapping
    public ProveedorDTO.Vista alta(@RequestBody @Valid ProveedorDTO.Alta datos) {
        return ProveedorDTO.Vista.de(servicio.alta(datos));
    }

    @PutMapping("/{id}")
    public ProveedorDTO.Vista editar(@PathVariable Long id, @RequestBody @Valid ProveedorDTO.Alta datos) {
        return ProveedorDTO.Vista.de(servicio.editar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
