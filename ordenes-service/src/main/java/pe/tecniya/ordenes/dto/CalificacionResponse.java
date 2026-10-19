package pe.tecniya.ordenes.dto;

import java.time.LocalDateTime;

import pe.tecniya.ordenes.domain.Calificacion;

public record CalificacionResponse(
        Long id,
        Long ordenId,
        int puntaje,
        String comentario,
        LocalDateTime fecha) {

    public static CalificacionResponse de(Calificacion c) {
        return new CalificacionResponse(c.getId(), c.getOrdenId(), c.getPuntaje(),
                c.getComentario(), c.getFecha());
    }
}
