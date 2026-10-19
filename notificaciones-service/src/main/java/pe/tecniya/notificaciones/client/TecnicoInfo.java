package pe.tecniya.notificaciones.client;

import java.math.BigDecimal;
import java.util.List;

/** Lo que solicitudes-service necesita saber del perfil de un técnico. */
public record TecnicoInfo(
        Long id,
        String estado,
        BigDecimal tarifaBase,
        List<String> especialidades) {
}
