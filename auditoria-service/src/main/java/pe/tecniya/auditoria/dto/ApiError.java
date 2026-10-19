package pe.tecniya.auditoria.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime fecha,
        int estado,
        String mensaje,
        List<String> detalles) {
}
