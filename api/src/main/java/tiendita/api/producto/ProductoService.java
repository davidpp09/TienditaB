package tiendita.api.producto;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiendita.api.categoria.CategoriaRepository;
import tiendita.api.infra.ReglaDeNegocioException;
import tiendita.api.kardex.MovimientoInventario;
import tiendita.api.kardex.MovimientoInventarioRepository;
import tiendita.api.kardex.TipoMovimiento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final MovimientoInventarioRepository kardex;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias,
                           MovimientoInventarioRepository kardex) {
        this.productos = productos;
        this.categorias = categorias;
        this.kardex = kardex;
    }

    public Optional<Producto> porCodigo(String codigo) {
        return productos.findByCodigoBarras(codigo);
    }

    public List<Producto> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return productos.findByActivoTrueOrderByNombre();
        }
        return productos.buscarPorNombre(texto.trim());
    }

    public Producto porId(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el producto " + id));
    }

    @Transactional
    public Producto altaRapida(ProductoDTO.AltaRapida datos) {
        String codigo = normalizarCodigo(datos.codigoBarras());
        if (codigo != null && productos.existsByCodigoBarras(codigo)) {
            throw new ReglaDeNegocioException("Ya existe un producto con el código " + codigo);
        }
        return productos.save(new Producto(codigo, datos.nombre().trim(), datos.unidad(), datos.precioVenta()));
    }

    @Transactional
    public Producto editar(Long id, ProductoDTO.Edicion datos) {
        Producto p = porId(id);
        String codigo = normalizarCodigo(datos.codigoBarras());
        if (codigo != null && productos.findByCodigoBarras(codigo).filter(o -> !o.getId().equals(id)).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe otro producto con el código " + codigo);
        }
        p.setCodigoBarras(codigo);
        p.setNombre(datos.nombre().trim());
        p.setUnidad(datos.unidad());
        p.setPrecioVenta(datos.precioVenta());
        p.setPrecioMayoreo(datos.precioMayoreo());
        p.setStockMinimo(datos.stockMinimo() == null ? BigDecimal.ZERO : datos.stockMinimo());
        if (datos.diasProveedor() != null) {
            p.setDiasProveedor(datos.diasProveedor());
        }
        if (datos.categoriaId() != null) {
            p.setCategoria(categorias.findById(datos.categoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("No existe la categoría " + datos.categoriaId())));
        }
        return p;
    }

    /**
     * Baja suave. Un producto con ventas nunca se borra: sus renglones tienen que
     * seguir explicando el pasado. (Misma lección que la V1 de RestFood.)
     */
    @Transactional
    public void desactivar(Long id) {
        porId(id).setActivo(false);
    }

    /** Salida por rotura, caducidad o robo. La cantidad se recibe en positivo. */
    @Transactional
    public Producto merma(Long id, BigDecimal cantidad, String motivo, String usuario) {
        return salida(id, TipoMovimiento.MERMA, cantidad, motivo, usuario);
    }

    /** Salida de lo que se llevó la casa. Aparte de la merma: no es una pérdida igual. */
    @Transactional
    public Producto autoconsumo(Long id, BigDecimal cantidad, String motivo, String usuario) {
        return salida(id, TipoMovimiento.AUTOCONSUMO, cantidad, motivo, usuario);
    }

    /**
     * Cuadre contra un conteo físico. Se recibe LA EXISTENCIA REAL contada, no la
     * diferencia: quien cuenta en el pasillo sabe cuántas hay, no cuántas sobran.
     * El sistema calcula la diferencia y la deja escrita en el kardex.
     */
    @Transactional
    public Producto ajustarPorConteo(Long id, BigDecimal existenciaReal, String motivo, String usuario) {
        Producto p = productos.findByIdConBloqueo(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el producto " + id));
        BigDecimal diferencia = existenciaReal.subtract(p.getExistencia());
        if (diferencia.signum() == 0) {
            return p;
        }
        p.moverExistencia(diferencia);
        kardex.save(new MovimientoInventario(p, TipoMovimiento.AJUSTE, diferencia, p.getCostoPromedio(),
                p.getExistencia(), "AJUSTE", null, motivo, usuario));
        return p;
    }

    private Producto salida(Long id, TipoMovimiento tipo, BigDecimal cantidad, String motivo, String usuario) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor que cero");
        }
        Producto p = productos.findByIdConBloqueo(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el producto " + id));
        BigDecimal conSigno = cantidad.negate();
        p.moverExistencia(conSigno);
        kardex.save(new MovimientoInventario(p, tipo, conSigno, p.getCostoPromedio(),
                p.getExistencia(), tipo.name(), null, motivo, usuario));
        return p;
    }

    /** Un código vacío es NULL: el granel no tiene código y "" no es un código. */
    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim();
    }
}
