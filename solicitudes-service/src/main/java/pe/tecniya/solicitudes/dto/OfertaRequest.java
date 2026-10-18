package pe.tecniya.solicitudes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RN-07: el precio debe ser mayor a cero. */
public record OfertaRequest(
        @NotNull @DecimalMin(value = "0.01",
                message = "debe ser mayor a cero") BigDecimal precio,
        @NotNull @Min(1) @Max(365) Integer plazoDias,
        @Size(max = 300) String mensaje) {
}
