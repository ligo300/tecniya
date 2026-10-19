package pe.tecniya.ordenes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalificacionRequest(
        @NotNull @Min(1) @Max(5) Integer puntaje,
        @Size(max = 300) String comentario) {
}
