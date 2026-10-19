package pe.tecniya.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import pe.tecniya.ordenes.domain.EstadoOrden;

public record CambioEstadoRequest(@NotNull EstadoOrden estado) {
}
