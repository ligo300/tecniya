package pe.tecniya.tecnicos.dto;

import java.math.BigDecimal;
import java.util.List;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.Tecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;

/** Vista pública del técnico: no incluye documento ni teléfono. */
public record TecnicoResponse(
        Long id,
        String nombres,
        String apellidos,
        String zona,
        BigDecimal tarifaBase,
        EstadoTecnico estado,
        BigDecimal califPromedio,
        int califCantidad,
        List<TipoEquipo> especialidades) {

    public static TecnicoResponse de(Tecnico t, List<TipoEquipo> especialidades) {
        return new TecnicoResponse(t.getId(), t.getNombres(), t.getApellidos(),
                t.getZona(), t.getTarifaBase(), t.getEstado(),
                t.getCalifPromedio(), t.getCalifCantidad(), especialidades);
    }
}
