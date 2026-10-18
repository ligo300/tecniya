package pe.tecniya.solicitudes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.tecniya.solicitudes.domain.TipoEquipo;

/** RN-05: tipo, marca o modelo, falla y presupuesto referencial mayor a cero. */
public record SolicitudRequest(
        @NotNull TipoEquipo tipoEquipo,
        @NotBlank @Size(max = 100) String marcaModelo,
        @NotBlank @Size(max = 500) String descripcionFalla,
        @NotNull @DecimalMin(value = "0.01",
                message = "debe ser mayor a cero") BigDecimal presupuestoRef) {
}
