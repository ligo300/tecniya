package pe.tecniya.tecnicos.dto;

import java.math.BigDecimal;
import java.util.List;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.Tecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;

/** Vista completa, solo para el propio técnico y para el administrador. */
public record TecnicoDetalleResponse(
        Long id,
        Long usuarioId,
        String nombres,
        String apellidos,
        String documento,
        String telefono,
        String zona,
        BigDecimal tarifaBase,
        EstadoTecnico estado,
        BigDecimal califPromedio,
        int califCantidad,
        List<TipoEquipo> especialidades) {

    public static TecnicoDetalleResponse de(Tecnico t, List<TipoEquipo> especialidades) {
        return new TecnicoDetalleResponse(t.getId(), t.getUsuarioId(), t.getNombres(),
                t.getApellidos(), t.getDocumento(), t.getTelefono(), t.getZona(),
                t.getTarifaBase(), t.getEstado(), t.getCalifPromedio(),
                t.getCalifCantidad(), especialidades);
    }
}
