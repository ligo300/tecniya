package pe.tecniya.solicitudes.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecniya.solicitudes.client.OrdenesClient;
import pe.tecniya.solicitudes.client.TecnicoInfo;
import pe.tecniya.solicitudes.client.TecnicosClient;
import pe.tecniya.solicitudes.domain.EstadoOferta;
import pe.tecniya.solicitudes.domain.EstadoSolicitud;
import pe.tecniya.solicitudes.domain.Oferta;
import pe.tecniya.solicitudes.domain.Solicitud;
import pe.tecniya.solicitudes.dto.OfertaRequest;
import pe.tecniya.solicitudes.dto.OfertaResponse;
import pe.tecniya.solicitudes.eventos.EventoPublisher;
import pe.tecniya.solicitudes.exception.AccesoDenegadoException;
import pe.tecniya.solicitudes.exception.ConflictoException;
import pe.tecniya.solicitudes.exception.RecursoNoEncontradoException;
import pe.tecniya.solicitudes.exception.ReglaNegocioException;
import pe.tecniya.solicitudes.repository.OfertaRepository;
import pe.tecniya.solicitudes.repository.SolicitudRepository;
import pe.tecniya.solicitudes.security.JwtUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OfertaService {

    private final SolicitudRepository solicitudes;
    private final OfertaRepository ofertas;
    private final TecnicosClient tecnicosClient;
    private final OrdenesClient ordenesClient;
    private final EventoPublisher eventos;

    public OfertaService(SolicitudRepository solicitudes, OfertaRepository ofertas,
                         TecnicosClient tecnicosClient, OrdenesClient ordenesClient,
                         EventoPublisher eventos) {
        this.solicitudes = solicitudes;
        this.ofertas = ofertas;
        this.tecnicosClient = tecnicosClient;
        this.ordenesClient = ordenesClient;
        this.eventos = eventos;
    }

    /** RN-06 y RN-07: ofertan técnicos ACTIVO con la especialidad, una sola vez. */
    @Transactional
    public Mono<OfertaResponse> crear(Long solicitudId, OfertaRequest datos, String autorizacion) {
        return buscarSolicitud(solicitudId)
                .<Oferta>flatMap(s -> {
                    if (!recibeOfertas(s)) {
                        return Mono.<Oferta>error(
                                new ReglaNegocioException("La solicitud ya no recibe ofertas"));
                    }
                    return tecnicosClient.miPerfil(autorizacion)
                            .flatMap(t -> {
                                validarTecnico(t, s);
                                return ofertas.existsBySolicitudIdAndTecnicoId(s.getId(), t.id())
                                        .flatMap(existe -> existe
                                                ? Mono.<Oferta>error(new ConflictoException(
                                                        "Ya enviaste una oferta a esta solicitud"))
                                                : ofertas.save(construir(datos, s, t)));
                            })
                            .flatMap(o -> marcarConOfertas(s).thenReturn(o));
                })
                .map(OfertaResponse::de);
    }

    /** El cliente dueño de la solicitud ve las ofertas, de menor a mayor precio. */
    public Flux<OfertaResponse> listarDeSolicitud(Long solicitudId, JwtUser usuario) {
        return buscarSolicitud(solicitudId)
                .flatMapMany(s -> {
                    if (!s.getClienteId().equals(usuario.id())) {
                        return Flux.<Oferta>error(new AccesoDenegadoException(
                                "Solo el cliente de la solicitud ve sus ofertas"));
                    }
                    return ofertas.findBySolicitudIdOrderByPrecioAsc(solicitudId);
                })
                .map(OfertaResponse::de);
    }

    @Transactional
    public Mono<OfertaResponse> actualizar(Long ofertaId, OfertaRequest datos, String autorizacion) {
        return buscarOferta(ofertaId)
                .flatMap(o -> verificarDuenoOferta(o, autorizacion)
                        .then(buscarSolicitud(o.getSolicitudId()))
                        .<Oferta>flatMap(s -> {
                            if (o.getEstado() != EstadoOferta.PENDIENTE || !recibeOfertas(s)) {
                                return Mono.<Oferta>error(new ReglaNegocioException(
                                        "La oferta ya no se puede modificar"));
                            }
                            o.setPrecio(datos.precio());
                            o.setPlazoDias(datos.plazoDias());
                            o.setMensaje(datos.mensaje());
                            return ofertas.save(o);
                        }))
                .map(OfertaResponse::de);
    }

    /** El técnico retira su oferta; si era la última, la solicitud vuelve a ABIERTA. */
    @Transactional
    public Mono<Void> retirar(Long ofertaId, String autorizacion) {
        return buscarOferta(ofertaId)
                .flatMap(o -> verificarDuenoOferta(o, autorizacion)
                        .then(buscarSolicitud(o.getSolicitudId()))
                        .flatMap(s -> {
                            if (o.getEstado() != EstadoOferta.PENDIENTE || !recibeOfertas(s)) {
                                return Mono.<Void>error(new ReglaNegocioException(
                                        "La oferta ya no se puede retirar"));
                            }
                            return ofertas.delete(o)
                                    .then(ofertas.countBySolicitudId(s.getId()))
                                    .flatMap(restantes -> {
                                        if (restantes == 0
                                                && s.getEstado() == EstadoSolicitud.CON_OFERTAS) {
                                            s.setEstado(EstadoSolicitud.ABIERTA);
                                            return solicitudes.save(s).then();
                                        }
                                        return Mono.<Void>empty();
                                    });
                        }));
    }

    /** RN-08: el cliente acepta una sola oferta y la solicitud queda ADJUDICADA. */
    @Transactional
    public Mono<OfertaResponse> aceptar(Long ofertaId, JwtUser usuario) {
        return buscarOferta(ofertaId)
                .flatMap(o -> buscarSolicitud(o.getSolicitudId())
                        .<Oferta>flatMap(s -> {
                            if (!s.getClienteId().equals(usuario.id())) {
                                return Mono.<Oferta>error(new AccesoDenegadoException(
                                        "Solo el cliente de la solicitud puede aceptar ofertas"));
                            }
                            if (o.getEstado() != EstadoOferta.PENDIENTE
                                    || s.getEstado() != EstadoSolicitud.CON_OFERTAS) {
                                return Mono.<Oferta>error(new ReglaNegocioException(
                                        "Esta oferta ya no se puede aceptar"));
                            }
                            o.setEstado(EstadoOferta.ACEPTADA);
                            s.setEstado(EstadoSolicitud.ADJUDICADA);
                            return ofertas.save(o)
                                    .then(solicitudes.save(s))
                                    .then(rechazarPendientes(s.getId(), o.getId()))
                                    .then(ordenesClient.crearOrdenDesdeOferta(s, o))
                                    .then(eventos.publicar("oferta.aceptada", "OfertaAceptada", Map.of(
                                            "solicitudId", s.getId(),
                                            "ofertaId", o.getId(),
                                            "clienteId", s.getClienteId(),
                                            "tecnicoId", o.getTecnicoId(),
                                            "precio", o.getPrecio())))
                                    .thenReturn(o);
                        }))
                .map(OfertaResponse::de);
    }

    /** Marca como RECHAZADA toda oferta PENDIENTE de la solicitud, salvo la indicada. */
    public Mono<Void> rechazarPendientes(Long solicitudId, Long exceptoOfertaId) {
        return ofertas.findBySolicitudIdOrderByPrecioAsc(solicitudId)
                .filter(o -> o.getEstado() == EstadoOferta.PENDIENTE
                        && !o.getId().equals(exceptoOfertaId))
                .concatMap(o -> {
                    o.setEstado(EstadoOferta.RECHAZADA);
                    return ofertas.save(o);
                })
                .then();
    }

    // ---------- Apoyo ----------

    private Mono<Solicitud> buscarSolicitud(Long id) {
        return solicitudes.findById(id).switchIfEmpty(Mono.error(
                new RecursoNoEncontradoException("Solicitud no encontrada")));
    }

    private Mono<Oferta> buscarOferta(Long id) {
        return ofertas.findById(id).switchIfEmpty(Mono.error(
                new RecursoNoEncontradoException("Oferta no encontrada")));
    }

    private boolean recibeOfertas(Solicitud s) {
        return s.getEstado() == EstadoSolicitud.ABIERTA
                || s.getEstado() == EstadoSolicitud.CON_OFERTAS;
    }

    private void validarTecnico(TecnicoInfo t, Solicitud s) {
        if (!"ACTIVO".equals(t.estado())) {
            throw new ReglaNegocioException("Tu perfil de técnico no está activo");
        }
        if (t.especialidades() == null
                || !t.especialidades().contains(s.getTipoEquipo().name())) {
            throw new ReglaNegocioException(
                    "No tienes la especialidad " + s.getTipoEquipo());
        }
    }

    /** Comprueba, consultando a tecnicos-service, que la oferta es del técnico del token. */
    private Mono<Void> verificarDuenoOferta(Oferta oferta, String autorizacion) {
        return tecnicosClient.miPerfil(autorizacion)
                .flatMap(t -> t.id().equals(oferta.getTecnicoId())
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(new AccesoDenegadoException(
                                "Solo puedes modificar tus propias ofertas")));
    }

    private Mono<Solicitud> marcarConOfertas(Solicitud s) {
        if (s.getEstado() == EstadoSolicitud.ABIERTA) {
            s.setEstado(EstadoSolicitud.CON_OFERTAS);
            return solicitudes.save(s);
        }
        return Mono.just(s);
    }

    private Oferta construir(OfertaRequest datos, Solicitud s, TecnicoInfo t) {
        Oferta o = new Oferta();
        o.setSolicitudId(s.getId());
        o.setTecnicoId(t.id());
        o.setPrecio(datos.precio());
        o.setPlazoDias(datos.plazoDias());
        o.setMensaje(datos.mensaje());
        o.setEstado(EstadoOferta.PENDIENTE);
        o.setCreadoEn(LocalDateTime.now());
        return o;
    }
}
