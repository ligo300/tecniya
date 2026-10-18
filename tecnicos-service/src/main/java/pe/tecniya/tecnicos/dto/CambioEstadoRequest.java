package pe.tecniya.tecnicos.dto;

import jakarta.validation.constraints.NotNull;
import pe.tecniya.tecnicos.domain.EstadoTecnico;

public record CambioEstadoRequest(@NotNull EstadoTecnico estado) {
}
