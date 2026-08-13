package tiendita.api.proveedor;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.infra.ReglaDeNegocioException;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedores;

    public ProveedorService(ProveedorRepository proveedores) {
        this.proveedores = proveedores;
    }

    public List<Proveedor> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return proveedores.findByActivoTrueOrderByNombre();
        }
        return proveedores.buscarPorNombre(texto.trim());
    }

    public Proveedor porId(Long id) {
        return proveedores.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el proveedor " + id));
    }

    @Transactional
    public Proveedor alta(ProveedorDTO.Alta datos) {
        String nombre = datos.nombre().trim();
        proveedores.findByNombre(nombre).ifPresent(p -> {
            throw new ReglaDeNegocioException("Ya existe el proveedor \"" + p.getNombre() + "\"");
        });
        Proveedor proveedor = new Proveedor(nombre);
        aplicar(proveedor, datos);
        return proveedores.save(proveedor);
    }

    @Transactional
    public Proveedor editar(Long id, ProveedorDTO.Alta datos) {
        Proveedor proveedor = porId(id);
        String nombre = datos.nombre().trim();
        proveedores.findByNombre(nombre)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaDeNegocioException("Ya existe otro proveedor con el nombre \"" + otro.getNombre() + "\"");
                });
        proveedor.setNombre(nombre);
        aplicar(proveedor, datos);
        return proveedor;
    }

    /**
     * Baja suave, igual que en productos: un proveedor con compras nunca se
     * borra, porque sus compras tienen que seguir explicando de dónde salió el
     * costo de cada producto.
     */
    @Transactional
    public void desactivar(Long id) {
        porId(id).setActivo(false);
    }

    private void aplicar(Proveedor proveedor, ProveedorDTO.Alta datos) {
        proveedor.setTelefono(datos.telefono());
        proveedor.setNotas(datos.notas());
        if (datos.diasEntrega() != null) {
            proveedor.setDiasEntrega(datos.diasEntrega());
        }
    }
}
