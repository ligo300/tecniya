package pe.tecniya.solicitudes.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("solicitud")
public class Solicitud {

    @Id
    private Long id;
    private Long clienteId;
    private TipoEquipo tipoEquipo;
    private String marcaModelo;
    private String descripcionFalla;
    private BigDecimal presupuestoRef;
    private EstadoSolicitud estado;
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public TipoEquipo getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(TipoEquipo tipoEquipo) { this.tipoEquipo = tipoEquipo; }

    public String getMarcaModelo() { return marcaModelo; }
    public void setMarcaModelo(String marcaModelo) { this.marcaModelo = marcaModelo; }

    public String getDescripcionFalla() { return descripcionFalla; }
    public void setDescripcionFalla(String descripcionFalla) { this.descripcionFalla = descripcionFalla; }

    public BigDecimal getPresupuestoRef() { return presupuestoRef; }
    public void setPresupuestoRef(BigDecimal presupuestoRef) { this.presupuestoRef = presupuestoRef; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
