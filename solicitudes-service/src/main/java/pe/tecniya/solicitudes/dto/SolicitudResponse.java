package pe.tecniya.solicitudes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.tecniya.solicitudes.domain.EstadoSolicitud;
import pe.tecniya.solicitudes.domain.Solicitud;
import pe.tecniya.solicitudes.domain.TipoEquipo;

public record SolicitudResponse(
        Long id,
        Long clienteId,
        TipoEquipo tipoEquipo,
        String marcaModelo,
        String descripcionFalla,
        BigDecimal presupuestoRef,
        EstadoSolicitud estado,
        LocalDateTime creadoEn) {

    public static SolicitudResponse de(Solicitud s) {
        return new SolicitudResponse(s.getId(), s.getClienteId(), s.getTipoEquipo(),
                s.getMarcaModelo(), s.getDescripcionFalla(), s.getPresupuestoRef(),
                s.getEstado(), s.getCreadoEn());
    }
}
