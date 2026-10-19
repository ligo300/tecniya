package pe.tecniya.notificaciones.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import pe.tecniya.notificaciones.client.TecnicosClient;
import pe.tecniya.notificaciones.domain.Notificacion;
import pe.tecniya.notificaciones.dto.NotificacionResponse;
import pe.tecniya.notificaciones.eventos.EventoTecniya;
import pe.tecniya.notificaciones.exception.AccesoDenegadoException;
import pe.tecniya.notificaciones.exception.RecursoNoEncontradoException;
import pe.tecniya.notificaciones.repository.NotificacionRepository;
import pe.tecniya.notificaciones.security.JwtUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificacionService {

    /** A quién pertenece una notificación: CLIENTE (usuario), TECNICO (perfil) o ADMIN (0). */
    record Destino(String tipo, Long id) {
    }

    private final NotificacionRepository notificaciones;
    private final TecnicosClient tecnicosClient;

    public NotificacionService(NotificacionRepository notificaciones, TecnicosClient tecnicosClient) {
        this.notificaciones = notificaciones;
        this.tecnicosClient = tecnicosClient;
    }

    // ---------- Desde los eventos ----------

    /** Convierte un evento de negocio en una o más notificaciones simuladas. */
    public Mono<Void> procesar(EventoTecniya evento) {
        String tipo = evento.tipo() == null ? "" : evento.tipo();
        Map<String, Object> d = evento.datos() == null ? Map.of() : evento.datos();

        List<Notificacion> lista = switch (tipo) {
            case "TecnicoRegistrado" -> List.of(
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "Tu perfil fue registrado y está pendiente de validación."),
                    nueva("ADMIN", 0L, tipo,
                            "Nuevo técnico pendiente de validar: " + texto(d, "nombre")));
            case "TecnicoActivado" -> List.of(
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "Tu perfil fue activado: ya apareces en el catálogo."));
            case "OfertaAceptada" -> List.of(
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "El cliente aceptó tu oferta de S/ " + texto(d, "precio") + "."));
            case "OrdenCreada" -> List.of(
                    nueva("CLIENTE", numero(d, "clienteId"), tipo,
                            "Tu orden " + texto(d, "codigo") + " fue creada."),
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "Tienes una nueva orden: " + texto(d, "codigo") + "."));
            case "OrdenCambioEstado" -> List.of(
                    nueva("CLIENTE", numero(d, "clienteId"), tipo,
                            "Tu orden " + texto(d, "codigo") + " cambió a "
                                    + texto(d, "estadoNuevo") + "."));
            case "PagoRegistrado" -> List.of(
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "Pago de S/ " + texto(d, "monto") + " en la orden "
                                    + texto(d, "codigo") + ": " + texto(d, "estado") + "."));
            case "OrdenCalificada" -> List.of(
                    nueva("TECNICO", numero(d, "tecnicoId"), tipo,
                            "Recibiste una calificación de " + texto(d, "puntaje")
                                    + "/5 en la orden " + texto(d, "codigo") + "."));
            default -> List.<Notificacion>of();
        };
        return notificaciones.saveAll(lista).then();
    }

    // ---------- Consulta desde la web ----------

    public Flux<NotificacionResponse> listar(JwtUser usuario, String autorizacion,
                                             boolean soloNoLeidas) {
        return destino(usuario, autorizacion)
                .flatMapMany(d -> soloNoLeidas
                        ? notificaciones.findByDestinatarioTipoAndDestinatarioIdAndLeidaOrderByIdDesc(
                                d.tipo(), d.id(), false)
                        : notificaciones.findByDestinatarioTipoAndDestinatarioIdOrderByIdDesc(
                                d.tipo(), d.id()))
                .map(NotificacionResponse::de);
    }

    public Mono<NotificacionResponse> marcarLeida(Long id, JwtUser usuario, String autorizacion) {
        return destino(usuario, autorizacion)
                .flatMap(d -> notificaciones.findById(id)
                        .switchIfEmpty(Mono.error(new RecursoNoEncontradoException(
                                "Notificación no encontrada")))
                        .flatMap(n -> {
                            if (!n.getDestinatarioTipo().equals(d.tipo())
                                    || !n.getDestinatarioId().equals(d.id())) {
                                return Mono.<Notificacion>error(new AccesoDenegadoException(
                                        "Esta notificación no es tuya"));
                            }
                            n.setLeida(true);
                            return notificaciones.save(n);
                        }))
                .map(NotificacionResponse::de);
    }

    // ---------- Apoyo ----------

    private Mono<Destino> destino(JwtUser usuario, String autorizacion) {
        switch (usuario.rol()) {
            case "CLIENTE":
                return Mono.just(new Destino("CLIENTE", usuario.id()));
            case "TECNICO":
                return tecnicosClient.miPerfil(autorizacion)
                        .map(p -> new Destino("TECNICO", p.id()));
            default:
                return Mono.just(new Destino("ADMIN", 0L));
        }
    }

    private Notificacion nueva(String tipo, Long id, String evento, String mensaje) {
        Notificacion n = new Notificacion();
        n.setDestinatarioTipo(tipo);
        n.setDestinatarioId(id);
        n.setEvento(evento);
        n.setMensaje(mensaje.length() > 300 ? mensaje.substring(0, 300) : mensaje);
        n.setLeida(false);
        n.setCreadaEn(LocalDateTime.now());
        return n;
    }

    private Long numero(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        if (v instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("Evento incompleto: falta " + clave);
    }

    private String texto(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        return v == null ? "" : String.valueOf(v);
    }
}
