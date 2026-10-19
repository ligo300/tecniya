package pe.tecniya.ordenes.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import pe.tecniya.ordenes.domain.Calificacion;
import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.dto.CalificacionRequest;
import pe.tecniya.ordenes.dto.CalificacionResponse;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ConflictoException;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.CalificacionRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;

/** RN-15: el cliente califica una sola vez, y solo cuando la orden fue ENTREGADA. */
@Service
public class CalificacionService {

    private final CalificacionRepository calificaciones;
    private final AccesoOrden acceso;
    private final EventoPublisher eventos;

    public CalificacionService(CalificacionRepository calificaciones, AccesoOrden acceso,
                               EventoPublisher eventos) {
        this.calificaciones = calificaciones;
        this.acceso = acceso;
        this.eventos = eventos;
    }

    public Mono<CalificacionResponse> calificar(Long ordenId, CalificacionRequest req,
                                                JwtUser usuario) {
        return acceso.comoCliente(ordenId, usuario).flatMap(o -> {
            if (o.getEstado() != EstadoOrden.ENTREGADO) {
                return Mono.<Calificacion>error(new ReglaNegocioException(
                        "Solo puedes calificar una orden ENTREGADA"));
            }
            return calificaciones.existsByOrdenId(ordenId).flatMap(ya -> {
                if (ya) {
                    return Mono.<Calificacion>error(new ConflictoException(
                            "Esta orden ya fue calificada"));
                }
                Calificacion c = new Calificacion();
                c.setOrdenId(ordenId);
                c.setPuntaje(req.puntaje());
                c.setComentario(req.comentario());
                c.setFecha(LocalDateTime.now());
                return calificaciones.save(c).flatMap(g -> eventos
                        .publicar("orden.calificada", "OrdenCalificada", Map.of(
                                "ordenId", o.getId(),
                                "codigo", o.getCodigo(),
                                "clienteId", o.getClienteId(),
                                "tecnicoId", o.getTecnicoId(),
                                "puntaje", g.getPuntaje()))
                        .thenReturn(g));
            });
        }).map(CalificacionResponse::de);
    }
}
