package pe.tecniya.solicitudes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.tecniya.solicitudes.domain.EstadoOferta;
import pe.tecniya.solicitudes.domain.Oferta;

public record OfertaResponse(
        Long id,
        Long solicitudId,
        Long tecnicoId,
        BigDecimal precio,
        int plazoDias,
        String mensaje,
        EstadoOferta estado,
        LocalDateTime creadoEn) {

    public static OfertaResponse de(Oferta o) {
        return new OfertaResponse(o.getId(), o.getSolicitudId(), o.getTecnicoId(),
                o.getPrecio(), o.getPlazoDias(), o.getMensaje(), o.getEstado(),
                o.getCreadoEn());
    }
}
