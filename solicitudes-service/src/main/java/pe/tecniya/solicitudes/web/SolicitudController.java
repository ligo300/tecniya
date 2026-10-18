package pe.tecniya.solicitudes.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.tecniya.solicitudes.dto.OfertaRequest;
import pe.tecniya.solicitudes.dto.OfertaResponse;
import pe.tecniya.solicitudes.dto.SolicitudRequest;
import pe.tecniya.solicitudes.dto.SolicitudResponse;
import pe.tecniya.solicitudes.security.UsuarioActual;
import pe.tecniya.solicitudes.service.OfertaService;
import pe.tecniya.solicitudes.service.SolicitudService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final OfertaService ofertaService;

    public SolicitudController(SolicitudService solicitudService, OfertaService ofertaService) {
        this.solicitudService = solicitudService;
        this.ofertaService = ofertaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SolicitudResponse> crear(@Valid @RequestBody SolicitudRequest datos) {
        return UsuarioActual.obtener().flatMap(u -> solicitudService.crear(datos, u));
    }

    /** Cliente: las suyas. Técnico: abiertas de su especialidad. */
    @GetMapping
    public Flux<SolicitudResponse> listar(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMapMany(u -> solicitudService.listar(u, autorizacion));
    }

    @GetMapping("/{id}")
    public Mono<SolicitudResponse> obtener(@PathVariable Long id) {
        return UsuarioActual.obtener().flatMap(u -> solicitudService.obtener(id, u));
    }

    @PutMapping("/{id}")
    public Mono<SolicitudResponse> actualizar(@PathVariable Long id,
                                              @Valid @RequestBody SolicitudRequest datos) {
        return UsuarioActual.obtener().flatMap(u -> solicitudService.actualizar(id, datos, u));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelar(@PathVariable Long id) {
        return UsuarioActual.obtener().flatMap(u -> solicitudService.cancelar(id, u));
    }

    @PostMapping("/{id}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OfertaResponse> ofertar(
            @PathVariable Long id,
            @Valid @RequestBody OfertaRequest datos,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return ofertaService.crear(id, datos, autorizacion);
    }

    @GetMapping("/{id}/ofertas")
    public Flux<OfertaResponse> ofertas(@PathVariable Long id) {
        return UsuarioActual.obtener()
                .flatMapMany(u -> ofertaService.listarDeSolicitud(id, u));
    }
}
