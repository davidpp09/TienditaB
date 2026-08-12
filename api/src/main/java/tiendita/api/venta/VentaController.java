package tiendita.api.venta;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService servicio;

    public VentaController(VentaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public VentaDTO.Vista cobrar(@RequestBody @Valid VentaDTO.Cobro cobro) {
        return VentaDTO.Vista.de(servicio.cobrar(cobro, usuario()));
    }

    @GetMapping("/{id}")
    public VentaDTO.Vista porId(@PathVariable Long id) {
        return servicio.vista(id);
    }

    @GetMapping
    public List<VentaDTO.Vista> ultimas() {
        return servicio.ultimas();
    }

    @PostMapping("/{id}/cancelar")
    public VentaDTO.Vista cancelar(@PathVariable Long id, @RequestBody VentaDTO.Cancelacion c) {
        return VentaDTO.Vista.de(servicio.cancelar(id, c.motivo(), usuario()));
    }

    /** Texto plano listo para la impresora térmica. */
    @GetMapping(value = "/{id}/ticket", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String ticket(@PathVariable Long id) {
        return servicio.ticket(id);
    }

    /** Vendido, costo y utilidad del día. Lo que entró no es lo que se ganó. */
    @GetMapping("/resumen")
    public ResumenVentas resumen(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        return servicio.resumenDelDia(dia == null ? LocalDate.now() : dia);
    }

    private String usuario() {
        return "mostrador";
    }
}
