package pe.tecniya.solicitudes.service;

import java.time.LocalDateTime;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecniya.solicitudes.client.TecnicosClient;
import pe.tecniya.solicitudes.domain.EstadoSolicitud;
import pe.tecniya.solicitudes.domain.Solicitud;
import pe.tecniya.solicitudes.dto.SolicitudRequest;
import pe.tecniya.solicitudes.dto.SolicitudResponse;
import pe.tecniya.solicitudes.exception.AccesoDenegadoException;
import pe.tecniya.solicitudes.exception.RecursoNoEncontradoException;
import pe.tecniya.solicitudes.exception.ReglaNegocioException;
import pe.tecniya.solicitudes.repository.SolicitudRepository;
import pe.tecniya.solicitudes.security.JwtUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudes;
    private final TecnicosClient tecnicosClient;
    private final OfertaService ofertaService;

    public SolicitudService(SolicitudRepository solicitudes, TecnicosClient tecnicosClient,
                            OfertaService ofertaService) {
        this.solicitudes = solicitudes;
        this.tecnicosClient = tecnicosClient;
        this.ofertaService = ofertaService;
    }

    /** RN-05: la solicitud nace ABIERTA. */
    @Transactional
    public Mono<SolicitudResponse> crear(SolicitudRequest datos, JwtUser usuario) {
        Solicitud s = new Solicitud();
        s.setClienteId(usuario.id());
        aplicar(s, datos);
        s.setEstado(EstadoSolicitud.ABIERTA);
        s.setCreadoEn(LocalDateTime.now());
        return solicitudes.save(s).map(SolicitudResponse::de);
    }

    /** Cliente: las suyas. Técnico: las abiertas de sus especialidades. */
    public Flux<SolicitudResponse> listar(JwtUser usuario, String autorizacion) {
        if ("CLIENTE".equals(usuario.rol())) {
            return solicitudes.findByClienteIdOrderByCreadoEnDesc(usuario.id())
                    .map(SolicitudResponse::de);
        }
        return tecnicosClient.miPerfil(autorizacion)
                .flatMapMany(t -> abiertas().filter(s -> t.especialidades() != null
                        && t.especialidades().contains(s.getTipoEquipo().name())))
                .map(SolicitudResponse::de);
    }

    public Mono<SolicitudResponse> obtener(Long id, JwtUser usuario) {
        return buscar(id)
                .flatMap(s -> {
                    boolean esDueno = s.getClienteId().equals(usuario.id());
                    boolean visibleParaTecnico = "TECNICO".equals(usuario.rol())
                            && esAbierta(s);
                    if (!esDueno && !visibleParaTecnico) {
                        return Mono.<Solicitud>error(
                                new RecursoNoEncontradoException("Solicitud no encontrada"));
                    }
                    return Mono.just(s);
                })
                .map(SolicitudResponse::de);
    }

    /** Solo se edita mientras esté ABIERTA (sin ofertas). */
    @Transactional
    public Mono<SolicitudResponse> actualizar(Long id, SolicitudRequest datos, JwtUser usuario) {
        return buscar(id)
                .<Solicitud>flatMap(s -> {
                    verificarDueno(s, usuario);
                    if (s.getEstado() != EstadoSolicitud.ABIERTA) {
                        return Mono.<Solicitud>error(new ReglaNegocioException(
                                "Solo se puede editar una solicitud ABIERTA"));
                    }
                    aplicar(s, datos);
                    return solicitudes.save(s);
                })
                .map(SolicitudResponse::de);
    }

    /** Cancela (baja lógica) y rechaza las ofertas pendientes. */
    @Transactional
    public Mono<Void> cancelar(Long id, JwtUser usuario) {
        return buscar(id)
                .<Solicitud>flatMap(s -> {
                    verificarDueno(s, usuario);
                    if (!esAbierta(s)) {
                        return Mono.<Solicitud>error(new ReglaNegocioException(
                                "Solo se puede cancelar una solicitud ABIERTA o CON_OFERTAS"));
                    }
                    s.setEstado(EstadoSolicitud.CANCELADA);
                    return solicitudes.save(s);
                })
                .flatMap(s -> ofertaService.rechazarPendientes(s.getId(), null));
    }

    // ---------- Apoyo ----------

    private Flux<Solicitud> abiertas() {
        return Flux.concat(
                        solicitudes.findByEstado(EstadoSolicitud.ABIERTA),
                        solicitudes.findByEstado(EstadoSolicitud.CON_OFERTAS))
                .sort(Comparator.comparing(Solicitud::getCreadoEn).reversed());
    }

    private Mono<Solicitud> buscar(Long id) {
        return solicitudes.findById(id).switchIfEmpty(Mono.error(
                new RecursoNoEncontradoException("Solicitud no encontrada")));
    }

    private boolean esAbierta(Solicitud s) {
        return s.getEstado() == EstadoSolicitud.ABIERTA
                || s.getEstado() == EstadoSolicitud.CON_OFERTAS;
    }

    private void verificarDueno(Solicitud s, JwtUser usuario) {
        if (!s.getClienteId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo puedes modificar tus propias solicitudes");
        }
    }

    private void aplicar(Solicitud s, SolicitudRequest datos) {
        s.setTipoEquipo(datos.tipoEquipo());
        s.setMarcaModelo(datos.marcaModelo());
        s.setDescripcionFalla(datos.descripcionFalla());
        s.setPresupuestoRef(datos.presupuestoRef());
    }
}
