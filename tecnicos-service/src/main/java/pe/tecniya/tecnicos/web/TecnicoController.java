package pe.tecniya.tecnicos.web;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;
import pe.tecniya.tecnicos.dto.CambioEstadoRequest;
import pe.tecniya.tecnicos.dto.TecnicoDetalleResponse;
import pe.tecniya.tecnicos.dto.TecnicoRequest;
import pe.tecniya.tecnicos.dto.TecnicoResponse;
import pe.tecniya.tecnicos.security.UsuarioActual;
import pe.tecniya.tecnicos.service.TecnicoService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final TecnicoService service;

    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    /** Público: catálogo de técnicos activos con filtros opcionales. */
    @GetMapping
    public Flux<TecnicoResponse> catalogo(
            @RequestParam(required = false) TipoEquipo tipo,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false) BigDecimal calificacionMin) {
        return service.catalogo(tipo, zona, calificacionMin);
    }

    /** TECNICO: su propio perfil, en cualquier estado. */
    @GetMapping("/mio")
    public Mono<TecnicoDetalleResponse> miPerfil() {
        return UsuarioActual.obtener().flatMap(service::miPerfil);
    }

    /** ADMIN: lista técnicos de cualquier estado (filtro opcional). */
    @GetMapping("/gestion")
    public Flux<TecnicoDetalleResponse> gestion(
            @RequestParam(required = false) EstadoTecnico estado) {
        return service.gestion(estado);
    }

    /** Público: perfil de un técnico activo. */
    @GetMapping("/{id}")
    public Mono<TecnicoResponse> obtener(@PathVariable Long id) {
        return service.obtenerPublico(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TecnicoDetalleResponse> crear(@Valid @RequestBody TecnicoRequest datos) {
        return UsuarioActual.obtener().flatMap(u -> service.crear(datos, u));
    }

    @PutMapping("/{id}")
    public Mono<TecnicoDetalleResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody TecnicoRequest datos) {
        return UsuarioActual.obtener().flatMap(u -> service.actualizar(id, datos, u));
    }

    @PutMapping("/{id}/estado")
    public Mono<TecnicoDetalleResponse> cambiarEstado(@PathVariable Long id,
                                                      @Valid @RequestBody CambioEstadoRequest datos) {
        return service.cambiarEstado(id, datos.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }
}
