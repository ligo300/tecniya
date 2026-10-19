package pe.tecniya.ordenes.dto;

import java.time.LocalDateTime;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.OrdenFoto;

public record FotoResponse(
        Long id,
        Long ordenId,
        EstadoOrden estado,
        String nombreArchivo,
        long tamanoBytes,
        Long subidoPor,
        LocalDateTime fecha,
        String rutaDescarga) {

    public static FotoResponse de(OrdenFoto f) {
        return new FotoResponse(f.getId(), f.getOrdenId(), f.getEstado(),
                f.getNombreArchivo(), f.getTamanoBytes(), f.getSubidoPor(), f.getFecha(),
                "/api/ordenes/" + f.getOrdenId() + "/fotos/" + f.getId());
    }
}
