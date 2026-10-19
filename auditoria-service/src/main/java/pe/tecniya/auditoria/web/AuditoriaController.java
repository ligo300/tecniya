package pe.tecniya.auditoria.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.tecniya.auditoria.dto.AuditoriaResponse;
import pe.tecniya.auditoria.service.AuditoriaService;
import reactor.core.publisher.Flux;

/** Solo el administrador consulta la auditoría (hasta 200 registros, del más nuevo al más antiguo). */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService servicio;

    public AuditoriaController(AuditoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Flux<AuditoriaResponse> listar(@RequestParam(required = false) String tipo) {
        return servicio.listar(tipo);
    }
}
