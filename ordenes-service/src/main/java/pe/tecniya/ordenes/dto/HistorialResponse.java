package pe.tecniya.ordenes.dto;

import java.time.LocalDateTime;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.OrdenHistorial;

public record HistorialResponse(
        EstadoOrden estadoAnterior,
        EstadoOrden estadoNuevo,
        Long usuarioId,
        LocalDateTime fecha) {

    public static HistorialResponse de(OrdenHistorial h) {
        return new HistorialResponse(h.getEstadoAnterior(), h.getEstadoNuevo(),
                h.getUsuarioId(), h.getFecha());
    }
}
