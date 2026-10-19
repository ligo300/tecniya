package pe.tecniya.ordenes.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** RN-14: cada cambio de estado queda registrado (solo inserción). */
@Table("orden_historial")
public class OrdenHistorial {

    @Id
    private Long id;
    private Long ordenId;
    private EstadoOrden estadoAnterior;
    private EstadoOrden estadoNuevo;
    private Long usuarioId;
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }

    public EstadoOrden getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoOrden estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public EstadoOrden getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoOrden estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
