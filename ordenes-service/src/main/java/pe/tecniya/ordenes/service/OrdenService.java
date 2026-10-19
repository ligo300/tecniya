package pe.tecniya.ordenes.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecniya.ordenes.client.TecnicosClient;
import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.OrdenHistorial;
import pe.tecniya.ordenes.domain.OrigenOrden;
import pe.tecniya.ordenes.dto.CalificacionResponse;
import pe.tecniya.ordenes.dto.FotoResponse;
import pe.tecniya.ordenes.dto.HistorialResponse;
import pe.tecniya.ordenes.dto.OrdenDetalleResponse;
import pe.tecniya.ordenes.dto.OrdenDirectaRequest;
import pe.tecniya.ordenes.dto.OrdenInternaRequest;
import pe.tecniya.ordenes.dto.OrdenResponse;
import pe.tecniya.ordenes.dto.PagoResponse;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.CalificacionRepository;
import pe.tecniya.ordenes.repository.FotoRepository;
import pe.tecniya.ordenes.repository.HistorialRepository;
import pe.tecniya.ordenes.repository.OrdenRepository;
import pe.tecniya.ordenes.repository.PagoRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrdenService {

    private final OrdenRepository ordenes;
    private final HistorialRepository historial;
    private final FotoRepository fotos;
    private final PagoRepository pagos;
    private final CalificacionRepository calificaciones;
    private final TecnicosClient tecnicosClient;
    private final AccesoOrden acceso;
    private final EventoPublisher eventos;

    public OrdenService(OrdenRepository ordenes, HistorialRepository historial,
                        FotoRepository fotos, PagoRepository pagos,
                        CalificacionRepository calificaciones,
                        TecnicosClient tecnicosClient, AccesoOrden acceso,
                        EventoPublisher eventos) {
        this.ordenes = ordenes;
        this.historial = historial;
        this.fotos = fotos;
        this.pagos = pagos;
        this.calificaciones = calificaciones;
        this.tecnicosClient = tecnicosClient;
        this.acceso = acceso;
        this.eventos = eventos;
    }

    /** Siguiente estado del flujo normal, o null si la orden ya terminó. */
    public static EstadoOrden siguiente(EstadoOrden actual) {
        if (actual == EstadoOrden.ENTREGADO || actual == EstadoOrden.CANCELADO) {
            return null;
        }
        return EstadoOrden.values()[actual.ordinal() + 1];
    }

    // ---------- Creación ----------

    /** Contratación directa: el cliente elige al técnico y paga su tarifa base. */
    @Transactional
    public Mono<OrdenResponse> crearDirecta(OrdenDirectaRequest req, JwtUser cliente) {
        return tecnicosClient.perfilPublico(req.tecnicoId()).flatMap(t -> {
            if (!"ACTIVO".equals(t.estado())) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "El técnico no está disponible para ser contratado"));
            }
            if (t.especialidades() == null
                    || !t.especialidades().contains(req.tipoEquipo().name())) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "El técnico no atiende ese tipo de equipo"));
            }
            if (t.tarifaBase() == null || t.tarifaBase().signum() <= 0) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "El técnico no tiene una tarifa base definida"));
            }
            Orden o = nueva(cliente.id(), t.id(), null, null, OrigenOrden.DIRECTA);
            o.setTipoEquipo(req.tipoEquipo());
            o.setDescripcion(req.descripcion());
            o.setPrecioAcordado(t.tarifaBase());
            return guardarNueva(o, cliente.id());
        }).map(OrdenResponse::de);
    }

    /** Llamada interna desde solicitudes-service. Es idempotente por ofertaId. */
    @Transactional
    public Mono<OrdenResponse> crearDesdeOferta(OrdenInternaRequest req) {
        return ordenes.findByOfertaId(req.ofertaId())
                .switchIfEmpty(Mono.defer(() -> {
                    Orden o = nueva(req.clienteId(), req.tecnicoId(), req.solicitudId(),
                            req.ofertaId(), OrigenOrden.OFERTA);
                    o.setTipoEquipo(req.tipoEquipo());
                    o.setDescripcion(req.descripcion());
                    o.setPrecioAcordado(req.precioAcordado());
                    return guardarNueva(o, req.clienteId());
                }))
                .map(OrdenResponse::de);
    }

    private Orden nueva(Long clienteId, Long tecnicoId, Long solicitudId, Long ofertaId,
                        OrigenOrden origen) {
        LocalDateTime ahora = LocalDateTime.now();
        Orden o = new Orden();
        o.setCodigo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        o.setClienteId(clienteId);
        o.setTecnicoId(tecnicoId);
        o.setSolicitudId(solicitudId);
        o.setOfertaId(ofertaId);
        o.setOrigen(origen);
        o.setEstado(EstadoOrden.CONTRATADO);
        o.setCreadoEn(ahora);
        o.setActualizadoEn(ahora);
        return o;
    }

    private Mono<Orden> guardarNueva(Orden o, Long usuarioId) {
        return ordenes.save(o).flatMap(g -> registrar(g.getId(), null,
                EstadoOrden.CONTRATADO, usuarioId)
                .then(eventos.publicar("orden.creada", "OrdenCreada", Map.of(
                        "ordenId", g.getId(),
                        "codigo", g.getCodigo(),
                        "clienteId", g.getClienteId(),
                        "tecnicoId", g.getTecnicoId(),
                        "origen", g.getOrigen().name(),
                        "precio", g.getPrecioAcordado())))
                .thenReturn(g));
    }

    private Mono<OrdenHistorial> registrar(Long ordenId, EstadoOrden anterior,
                                           EstadoOrden nuevo, Long usuarioId) {
        OrdenHistorial h = new OrdenHistorial();
        h.setOrdenId(ordenId);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setUsuarioId(usuarioId);
        h.setFecha(LocalDateTime.now());
        return historial.save(h);
    }

    // ---------- Consulta ----------

    public Flux<OrdenResponse> listar(JwtUser usuario, String autorizacion) {
        Flux<Orden> lista;
        switch (usuario.rol()) {
            case "CLIENTE":
                lista = ordenes.findByClienteIdOrderByCreadoEnDesc(usuario.id());
                break;
            case "TECNICO":
                lista = tecnicosClient.miPerfil(autorizacion).flatMapMany(p ->
                        ordenes.findByTecnicoIdOrderByCreadoEnDesc(p.id()));
                break;
            default:
                lista = ordenes.findAllByOrderByCreadoEnDesc();
        }
        return lista.map(OrdenResponse::de);
    }

    public Mono<OrdenDetalleResponse> detalle(Long id, JwtUser usuario, String autorizacion) {
        return acceso.comoParticipante(id, usuario, autorizacion).flatMap(o ->
                Mono.zip(
                        historial.findByOrdenIdOrderByIdAsc(id)
                                .map(HistorialResponse::de).collectList(),
                        fotos.findByOrdenIdOrderByIdAsc(id)
                                .map(FotoResponse::de).collectList(),
                        pagos.findByOrdenIdOrderByIdAsc(id)
                                .map(PagoResponse::de).collectList(),
                        calificaciones.findByOrdenId(id)
                                .map(CalificacionResponse::de)
                                .map(Optional::of).defaultIfEmpty(Optional.empty()))
                        .map(t -> new OrdenDetalleResponse(OrdenResponse.de(o),
                                t.getT1(), t.getT2(), t.getT3(),
                                t.getT4().orElse(null))));
    }

    // ---------- Cambios de estado ----------

    /** RN-11/12/19: solo el siguiente estado, con foto previa y pago antes de entregar. */
    @Transactional
    public Mono<OrdenResponse> cambiarEstado(Long id, EstadoOrden nuevo, JwtUser usuario,
                                             String autorizacion) {
        return acceso.comoTecnicoAsignado(id, autorizacion).flatMap(o -> {
            if (nuevo == EstadoOrden.CANCELADO) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "Para cancelar usa la acción de cancelar la orden"));
            }
            if (nuevo != siguiente(o.getEstado())) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "Transición no permitida: " + o.getEstado() + " → " + nuevo));
            }
            return exigirEvidencia(o, nuevo).then(Mono.defer(() -> {
                EstadoOrden anterior = o.getEstado();
                o.setEstado(nuevo);
                o.setActualizadoEn(LocalDateTime.now());
                return ordenes.save(o).flatMap(g ->
                        registrar(g.getId(), anterior, nuevo, usuario.id())
                                .then(eventos.publicar("orden.estado.cambiado", "OrdenCambioEstado",
                                        datosCambio(g, anterior, nuevo, usuario.id())))
                                .thenReturn(g));
            }));
        }).map(OrdenResponse::de);
    }

    private Mono<Void> exigirEvidencia(Orden o, EstadoOrden nuevo) {
        if (nuevo == EstadoOrden.ENTREGADO) {
            return pagos.existsByOrdenIdAndEstado(o.getId(), EstadoPago.PAGADO)
                    .flatMap(pagado -> pagado ? Mono.<Void>empty()
                            : Mono.<Void>error(new ReglaNegocioException(
                                    "No se puede entregar: la orden no tiene un pago confirmado")));
        }
        return fotos.existsByOrdenIdAndEstado(o.getId(), nuevo)
                .flatMap(hay -> hay ? Mono.<Void>empty()
                        : Mono.<Void>error(new ReglaNegocioException(
                                "Sube al menos una foto del estado " + nuevo
                                        + " antes de cambiarlo")));
    }

    /** El cliente cancela solo si aún está CONTRATADO; el técnico hasta EN_REPARACION. */
    @Transactional
    public Mono<OrdenResponse> cancelar(Long id, JwtUser usuario, String autorizacion) {
        Mono<Orden> origen = "CLIENTE".equals(usuario.rol())
                ? acceso.comoCliente(id, usuario)
                : acceso.comoTecnicoAsignado(id, autorizacion);
        return origen.flatMap(o -> {
            boolean permitido = "CLIENTE".equals(usuario.rol())
                    ? o.getEstado() == EstadoOrden.CONTRATADO
                    : o.getEstado().ordinal() <= EstadoOrden.EN_REPARACION.ordinal();
            if (!permitido) {
                return Mono.<Orden>error(new ReglaNegocioException(
                        "La orden ya no se puede cancelar en estado " + o.getEstado()));
            }
            EstadoOrden anterior = o.getEstado();
            o.setEstado(EstadoOrden.CANCELADO);
            o.setActualizadoEn(LocalDateTime.now());
            return ordenes.save(o).flatMap(g ->
                    registrar(g.getId(), anterior, EstadoOrden.CANCELADO, usuario.id())
                            .then(eventos.publicar("orden.estado.cambiado", "OrdenCambioEstado",
                                    datosCambio(g, anterior, EstadoOrden.CANCELADO, usuario.id())))
                            .thenReturn(g));
        }).map(OrdenResponse::de);
    }

    private Map<String, Object> datosCambio(Orden o, EstadoOrden anterior, EstadoOrden nuevo,
                                            Long usuarioId) {
        return Map.of(
                "ordenId", o.getId(),
                "codigo", o.getCodigo(),
                "clienteId", o.getClienteId(),
                "tecnicoId", o.getTecnicoId(),
                "estadoAnterior", anterior.name(),
                "estadoNuevo", nuevo.name(),
                "usuarioId", usuarioId);
    }
}
