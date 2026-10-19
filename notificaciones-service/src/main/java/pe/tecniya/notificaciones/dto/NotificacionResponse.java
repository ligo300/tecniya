package pe.tecniya.notificaciones.dto;

import java.time.LocalDateTime;

import pe.tecniya.notificaciones.domain.Notificacion;

public record NotificacionResponse(
        Long id,
        String evento,
        String mensaje,
        boolean leida,
        LocalDateTime creadaEn) {

    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getEvento(), n.getMensaje(),
                n.isLeida(), n.getCreadaEn());
    }
}
