package pe.tecniya.auditoria.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Registro de solo inserción: qué pasó, sobre qué entidad y con qué datos. */
@Table("auditoria")
public class Auditoria {

    @Id
    private Long id;
    private String tipo;
    private String entidad;
    private Long entidadId;
    private String datos;
    private LocalDateTime ocurridoEn;
    private LocalDateTime registradoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }

    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }

    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }

    public LocalDateTime getRegistradoEn() { return registradoEn; }
    public void setRegistradoEn(LocalDateTime registradoEn) { this.registradoEn = registradoEn; }
}
