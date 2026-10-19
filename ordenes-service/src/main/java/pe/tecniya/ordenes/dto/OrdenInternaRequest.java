package pe.tecniya.ordenes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.tecniya.ordenes.domain.TipoEquipo;

/** Lo que envía solicitudes-service cuando el cliente acepta una oferta. */
public record OrdenInternaRequest(
        @NotNull Long clienteId,
        @NotNull Long tecnicoId,
        @NotNull Long solicitudId,
        @NotNull Long ofertaId,
        @NotNull TipoEquipo tipoEquipo,
        @NotBlank @Size(max = 700) String descripcion,
        @NotNull @DecimalMin("0.01") BigDecimal precioAcordado) {
}
