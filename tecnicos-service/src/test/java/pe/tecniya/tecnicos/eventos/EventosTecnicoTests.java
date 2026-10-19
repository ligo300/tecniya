package pe.tecniya.tecnicos.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;
import pe.tecniya.tecnicos.dto.TecnicoDetalleResponse;
import pe.tecniya.tecnicos.dto.TecnicoRequest;
import pe.tecniya.tecnicos.repository.TecnicoRepository;
import pe.tecniya.tecnicos.security.JwtUser;
import pe.tecniya.tecnicos.service.TecnicoService;

/** Eventos de tecnicos-service: publica registro/activación y consume calificaciones (RN-20). */
@SpringBootTest
@ActiveProfiles("test")
class EventosTecnicoTests {

    @Autowired
    private TecnicoService servicio;
    @Autowired
    private TecnicoRepository repositorio;
    @Autowired
    private EventoPublisherEnMemoria eventos;
    @Autowired
    private CalificacionConsumidor consumidor;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
        eventos.limpiar();
    }

    private TecnicoDetalleResponse crearTecnico() {
        TecnicoRequest datos = new TecnicoRequest("Luis", "Pérez", "12345678", "999111222",
                "Lima", new BigDecimal("50"), List.of(TipoEquipo.PC));
        return servicio.crear(datos, new JwtUser(10L, "luis@correo.com", "TECNICO")).block();
    }

    private EventoTecniya calificacion(Object tecnicoId, int puntaje) {
        return new EventoTecniya("OrdenCalificada", LocalDateTime.now().toString(),
                Map.of("ordenId", 1, "tecnicoId", tecnicoId, "puntaje", puntaje));
    }

    @Test
    void registrarUnTecnicoPublicaTecnicoRegistrado() {
        TecnicoDetalleResponse t = crearTecnico();

        assertEquals(List.of("tecnico.registrado"), eventos.claves());
        Map<String, Object> datos = eventos.publicados().get(0).evento().datos();
        assertEquals(t.id(), ((Number) datos.get("tecnicoId")).longValue());
        assertEquals("Luis Pérez", datos.get("nombre"));
    }

    @Test
    void soloActivarPublicaTecnicoActivado() {
        TecnicoDetalleResponse t = crearTecnico();
        eventos.limpiar();

        servicio.cambiarEstado(t.id(), EstadoTecnico.ACTIVO).block();
        assertEquals(List.of("tecnico.activado"), eventos.claves());

        servicio.cambiarEstado(t.id(), EstadoTecnico.SUSPENDIDO).block();
        assertEquals(1, eventos.claves().size());
    }

    @Test
    void lasCalificacionesActualizanPromedioYCantidad() {
        TecnicoDetalleResponse t = crearTecnico();

        consumidor.alRecibir(calificacion(t.id(), 5));
        assertEquals(0, new BigDecimal("5.00").compareTo(promedio(t.id())));
        assertEquals(1, cantidad(t.id()));

        consumidor.alRecibir(calificacion(t.id(), 4));
        assertEquals(0, new BigDecimal("4.50").compareTo(promedio(t.id())));
        assertEquals(2, cantidad(t.id()));

        consumidor.alRecibir(calificacion(t.id(), 4));
        assertEquals(0, new BigDecimal("4.33").compareTo(promedio(t.id())));
        assertEquals(3, cantidad(t.id()));
    }

    @Test
    void ignoraEventosDeOtroTipoYTecnicosInexistentes() {
        TecnicoDetalleResponse t = crearTecnico();

        consumidor.alRecibir(new EventoTecniya("OrdenCreada", LocalDateTime.now().toString(),
                Map.of("tecnicoId", t.id(), "puntaje", 1)));
        consumidor.alRecibir(calificacion(99999L, 3));

        assertEquals(0, cantidad(t.id()));
        assertNotNull(repositorio.findById(t.id()).block());
    }

    private BigDecimal promedio(Long id) {
        return repositorio.findById(id).block().getCalifPromedio();
    }

    private int cantidad(Long id) {
        return repositorio.findById(id).block().getCalifCantidad();
    }
}
