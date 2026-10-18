package pe.tecniya.tecnicos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("tecnico")
public class Tecnico {

    @Id
    private Long id;
    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String documento;
    private String telefono;
    private String zona;
    private BigDecimal tarifaBase;
    private EstadoTecnico estado;
    private BigDecimal califPromedio;
    private int califCantidad;
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public BigDecimal getTarifaBase() { return tarifaBase; }
    public void setTarifaBase(BigDecimal tarifaBase) { this.tarifaBase = tarifaBase; }

    public EstadoTecnico getEstado() { return estado; }
    public void setEstado(EstadoTecnico estado) { this.estado = estado; }

    public BigDecimal getCalifPromedio() { return califPromedio; }
    public void setCalifPromedio(BigDecimal califPromedio) { this.califPromedio = califPromedio; }

    public int getCalifCantidad() { return califCantidad; }
    public void setCalifCantidad(int califCantidad) { this.califCantidad = califCantidad; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
