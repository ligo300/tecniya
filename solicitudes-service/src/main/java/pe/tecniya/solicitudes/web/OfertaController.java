package pe.tecniya.solicitudes.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pe.tecniya.solicitudes.security.UsuarioActual;
import pe.tecniya.solicitudes.service.OfertaService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService service;

    public OfertaController(OfertaService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public Mono<OfertaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody OfertaRequest datos,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return service.actualizar(id, datos, autorizacion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> retirar(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return service.retirar(id, autorizacion);
    }

    @PostMapping("/{id}/aceptar")
    public Mono<OfertaResponse> aceptar(@PathVariable Long id) {
        return UsuarioActual.obtener().flatMap(u -> service.aceptar(id, u));
    }
}
