package pe.tecniya.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.tecniya.ordenes.domain.TipoEquipo;

/** Contratación directa de un técnico (sin pasar por ofertas). */
public record OrdenDirectaRequest(
        @NotNull Long tecnicoId,
        @NotNull TipoEquipo tipoEquipo,
        @NotBlank @Size(max = 700) String descripcion) {
}
