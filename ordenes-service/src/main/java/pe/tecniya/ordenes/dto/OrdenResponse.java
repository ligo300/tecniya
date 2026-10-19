package pe.tecniya.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.OrigenOrden;
import pe.tecniya.ordenes.domain.TipoEquipo;

public record OrdenResponse(
        Long id,
        String codigo,
        Long clienteId,
        Long tecnicoId,
        Long solicitudId,
        Long ofertaId,
        OrigenOrden origen,
        TipoEquipo tipoEquipo,
        String descripcion,
        BigDecimal precioAcordado,
        EstadoOrden estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    public static OrdenResponse de(Orden o) {
        return new OrdenResponse(o.getId(), o.getCodigo(), o.getClienteId(),
                o.getTecnicoId(), o.getSolicitudId(), o.getOfertaId(), o.getOrigen(),
                o.getTipoEquipo(), o.getDescripcion(), o.getPrecioAcordado(),
                o.getEstado(), o.getCreadoEn(), o.getActualizadoEn());
    }
}
