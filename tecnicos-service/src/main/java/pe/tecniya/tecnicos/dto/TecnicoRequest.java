package pe.tecniya.tecnicos.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.tecniya.tecnicos.domain.TipoEquipo;

public record TecnicoRequest(
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @NotBlank @Pattern(regexp = "\\d{8,12}",
                message = "debe tener entre 8 y 12 dígitos") String documento,
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(max = 80) String zona,
        @NotNull @DecimalMin(value = "0.01",
                message = "debe ser mayor a cero") BigDecimal tarifaBase,
        @NotEmpty List<TipoEquipo> especialidades) {
}
