package pe.tecniya.ordenes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.tecniya.ordenes.domain.MedioPago;

public record PagoRequest(
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull MedioPago medio,
        @NotBlank @Size(max = 40) String nroOperacion) {
}
