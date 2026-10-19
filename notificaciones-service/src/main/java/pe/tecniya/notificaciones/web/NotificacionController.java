package pe.tecniya.notificaciones.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.tecniya.notificaciones.dto.NotificacionResponse;
import pe.tecniya.notificaciones.security.UsuarioActual;
import pe.tecniya.notificaciones.service.NotificacionService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService servicio;

    public NotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Flux<NotificacionResponse> listar(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMapMany(u -> servicio.listar(u, autorizacion, soloNoLeidas));
    }

    @PutMapping("/{id}/leida")
    public Mono<NotificacionResponse> marcarLeida(@PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> servicio.marcarLeida(id, u, autorizacion));
    }
}
