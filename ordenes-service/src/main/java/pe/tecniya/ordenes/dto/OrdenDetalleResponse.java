package pe.tecniya.ordenes.dto;

import java.util.List;

/** Orden con su trazabilidad completa: historial, fotos, pagos y calificación. */
public record OrdenDetalleResponse(
        OrdenResponse orden,
        List<HistorialResponse> historial,
        List<FotoResponse> fotos,
        List<PagoResponse> pagos,
        CalificacionResponse calificacion) {
}
