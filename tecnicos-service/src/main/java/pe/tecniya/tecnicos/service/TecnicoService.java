package pe.tecniya.tecnicos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.Tecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;
import pe.tecniya.tecnicos.dto.TecnicoDetalleResponse;
import pe.tecniya.tecnicos.dto.TecnicoRequest;
import pe.tecniya.tecnicos.dto.TecnicoResponse;
import pe.tecniya.tecnicos.exception.AccesoDenegadoException;
import pe.tecniya.tecnicos.exception.ConflictoException;
import pe.tecniya.tecnicos.exception.RecursoNoEncontradoException;
import pe.tecniya.tecnicos.exception.ReglaNegocioException;
import pe.tecniya.tecnicos.repository.EspecialidadRepository;
import pe.tecniya.tecnicos.repository.TecnicoRepository;
import pe.tecniya.tecnicos.security.JwtUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicos;
    private final EspecialidadRepository especialidades;

    public TecnicoService(TecnicoRepository tecnicos, EspecialidadRepository especialidades) {
        this.tecnicos = tecnicos;
        this.especialidades = especialidades;
    }

    // ---------- Consultas ----------

    /** RN-03: el catálogo público solo muestra técnicos ACTIVO. */
    public Flux<TecnicoResponse> catalogo(TipoEquipo tipo, String zona, BigDecimal calificacionMin) {
        return tecnicos.findByEstadoOrderByCalifPromedioDesc(EstadoTecnico.ACTIVO)
                .filter(t -> zona == null || zona.isBlank()
                        || t.getZona().toLowerCase().contains(zona.trim().toLowerCase()))
                .filter(t -> calificacionMin == null
                        || t.getCalifPromedio().compareTo(calificacionMin) >= 0)
                .concatMap(this::aPublico)
                .filter(r -> tipo == null || r.especialidades().contains(tipo));
    }

    public Mono<TecnicoResponse> obtenerPublico(Long id) {
        return tecnicos.findById(id)
                .filter(t -> t.getEstado() == EstadoTecnico.ACTIVO)
                .switchIfEmpty(Mono.error(noEncontrado()))
                .flatMap(this::aPublico);
    }

    public Mono<TecnicoDetalleResponse> miPerfil(JwtUser usuario) {
        return tecnicos.findByUsuarioId(usuario.id())
                .switchIfEmpty(Mono.error(
                        new RecursoNoEncontradoException("Aún no has creado tu perfil de técnico")))
                .flatMap(this::aDetalle);
    }

    public Flux<TecnicoDetalleResponse> gestion(EstadoTecnico estado) {
        Flux<Tecnico> origen = estado == null ? tecnicos.findAll() : tecnicos.findByEstado(estado);
        return origen.concatMap(this::aDetalle);
    }

    // ---------- Escritura ----------

    /** RN-03 y RN-04: el perfil nace PENDIENTE_VALIDACION. */
    @Transactional
    public Mono<TecnicoDetalleResponse> crear(TecnicoRequest datos, JwtUser usuario) {
        return tecnicos.existsByUsuarioId(usuario.id())
                .flatMap(existe -> existe
                        ? Mono.<Boolean>error(new ConflictoException("Ya tienes un perfil de técnico"))
                        : tecnicos.existsByDocumento(datos.documento()))
                .flatMap(duplicado -> duplicado
                        ? Mono.<Tecnico>error(new ConflictoException(
                                "Ya existe un técnico con el documento " + datos.documento()))
                        : Mono.just(construir(datos, usuario)))
                .flatMap(tecnicos::save)
                .flatMap(guardado -> especialidades
                        .reemplazar(guardado.getId(), datos.especialidades())
                        .thenReturn(guardado))
                .flatMap(this::aDetalle);
    }

    @Transactional
    public Mono<TecnicoDetalleResponse> actualizar(Long id, TecnicoRequest datos, JwtUser usuario) {
        return buscar(id)
                .<Tecnico>flatMap(t -> {
                    verificarDueno(t, usuario);
                    if (!t.getDocumento().equals(datos.documento())) {
                        return Mono.<Tecnico>error(
                                new ReglaNegocioException("El documento no se puede modificar"));
                    }
                    t.setNombres(datos.nombres());
                    t.setApellidos(datos.apellidos());
                    t.setTelefono(datos.telefono());
                    t.setZona(datos.zona());
                    t.setTarifaBase(datos.tarifaBase());
                    return tecnicos.save(t);
                })
                .flatMap(t -> especialidades.reemplazar(t.getId(), datos.especialidades())
                        .thenReturn(t))
                .flatMap(this::aDetalle);
    }

    /** Transiciones válidas: PENDIENTE→ACTIVO, ACTIVO→SUSPENDIDO, SUSPENDIDO→ACTIVO. */
    @Transactional
    public Mono<TecnicoDetalleResponse> cambiarEstado(Long id, EstadoTecnico nuevo) {
        return buscar(id)
                .<Tecnico>flatMap(t -> {
                    if (!transicionValida(t.getEstado(), nuevo)) {
                        return Mono.<Tecnico>error(new ReglaNegocioException(
                                "No se puede pasar de " + t.getEstado() + " a " + nuevo));
                    }
                    t.setEstado(nuevo);
                    return tecnicos.save(t);
                })
                .flatMap(this::aDetalle);
    }

    @Transactional
    public Mono<Void> eliminar(Long id) {
        return buscar(id).flatMap(tecnicos::delete);
    }

    // ---------- Apoyo ----------

    private Mono<Tecnico> buscar(Long id) {
        return tecnicos.findById(id).switchIfEmpty(Mono.error(noEncontrado()));
    }

    private RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException("Técnico no encontrado");
    }

    private void verificarDueno(Tecnico tecnico, JwtUser usuario) {
        boolean esAdmin = "ADMIN".equals(usuario.rol());
        if (!esAdmin && !tecnico.getUsuarioId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo puedes modificar tu propio perfil");
        }
    }

    private boolean transicionValida(EstadoTecnico actual, EstadoTecnico nuevo) {
        return (actual == EstadoTecnico.PENDIENTE_VALIDACION && nuevo == EstadoTecnico.ACTIVO)
                || (actual == EstadoTecnico.ACTIVO && nuevo == EstadoTecnico.SUSPENDIDO)
                || (actual == EstadoTecnico.SUSPENDIDO && nuevo == EstadoTecnico.ACTIVO);
    }

    private Tecnico construir(TecnicoRequest datos, JwtUser usuario) {
        Tecnico t = new Tecnico();
        t.setUsuarioId(usuario.id());
        t.setNombres(datos.nombres());
        t.setApellidos(datos.apellidos());
        t.setDocumento(datos.documento());
        t.setTelefono(datos.telefono());
        t.setZona(datos.zona());
        t.setTarifaBase(datos.tarifaBase());
        t.setEstado(EstadoTecnico.PENDIENTE_VALIDACION);
        t.setCalifPromedio(BigDecimal.ZERO);
        t.setCalifCantidad(0);
        t.setCreadoEn(LocalDateTime.now());
        return t;
    }

    private Mono<List<TipoEquipo>> especialidadesDe(Tecnico t) {
        return especialidades.listar(t.getId()).collectList();
    }

    private Mono<TecnicoResponse> aPublico(Tecnico t) {
        return especialidadesDe(t).map(lista -> TecnicoResponse.de(t, lista));
    }

    private Mono<TecnicoDetalleResponse> aDetalle(Tecnico t) {
        return especialidadesDe(t).map(lista -> TecnicoDetalleResponse.de(t, lista));
    }
}
