package pe.tecniya.ordenes.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** RN-13: la foto queda asociada al estado de destino; no se edita una vez subida. */
@Table("orden_foto")
public class OrdenFoto {

    @Id
    private Long id;
    private Long ordenId;
    private EstadoOrden estado;
    /** Ruta del archivo dentro de la carpeta de uploads (no es una URL pública). */
    private String url;
    private String nombreArchivo;
    private long tamanoBytes;
    private Long subidoPor;
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }

    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public Long getSubidoPor() { return subidoPor; }
    public void setSubidoPor(Long subidoPor) { this.subidoPor = subidoPor; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
