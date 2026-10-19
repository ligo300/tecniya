package pe.tecniya.notificaciones.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Notificación simulada: no se envía por correo ni SMS, solo se guarda para mostrarla en la web.
 * destinatarioTipo: CLIENTE (id de usuario), TECNICO (id del perfil de técnico) o ADMIN (id 0).
 */
@Table("notificacion")
public class Notificacion {

    @Id
    private Long id;
    private String destinatarioTipo;
    private Long destinatarioId;
    private String evento;
    private String mensaje;
    private boolean leida;
    private LocalDateTime creadaEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDestinatarioTipo() { return destinatarioTipo; }
    public void setDestinatarioTipo(String destinatarioTipo) { this.destinatarioTipo = destinatarioTipo; }

    public Long getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(Long destinatarioId) { this.destinatarioId = destinatarioId; }

    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
