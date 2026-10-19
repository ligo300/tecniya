package pe.tecniya.auditoria.dto;

import java.time.LocalDateTime;

import pe.tecniya.auditoria.domain.Auditoria;

public record AuditoriaResponse(
        Long id,
        String tipo,
        String entidad,
        Long entidadId,
        String datos,
        LocalDateTime ocurridoEn,
        LocalDateTime registradoEn) {

    public static AuditoriaResponse de(Auditoria a) {
        return new AuditoriaResponse(a.getId(), a.getTipo(), a.getEntidad(), a.getEntidadId(),
                a.getDatos(), a.getOcurridoEn(), a.getRegistradoEn());
    }
}
