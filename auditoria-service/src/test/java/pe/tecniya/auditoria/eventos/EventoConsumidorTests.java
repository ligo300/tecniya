package pe.tecniya.auditoria.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.auditoria.domain.Auditoria;
import pe.tecniya.auditoria.repository.AuditoriaRepository;

/** Cómo se guarda cada evento (sin RabbitMQ: se llama al consumidor directamente). */
@SpringBootTest
@ActiveProfiles("test")
class EventoConsumidorTests {

    @Autowired
    private EventoConsumidor consumidor;
    @Autowired
    private AuditoriaRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
    }

    private List<Auditoria> guardadas() {
        return repositorio.findAll().collectList().block();
    }

    @Test
    void guardaElEventoDeUnaOrdenConSuEntidadYSusDatos() {
        LocalDateTime ocurrio = LocalDateTime.of(2026, 10, 19, 15, 0, 0);
        consumidor.recibir(new EventoTecniya("OrdenCambioEstado", ocurrio.toString(),
                Map.of("ordenId", 12, "codigo", "ORD-AAAA1111", "estadoNuevo", "RECIBIDO")));

        Auditoria a = guardadas().get(0);
        assertEquals("OrdenCambioEstado", a.getTipo());
        assertEquals("ORDEN", a.getEntidad());
        assertEquals(12L, a.getEntidadId());
        assertEquals(ocurrio, a.getOcurridoEn());
        assertTrue(a.getDatos().contains("ORD-AAAA1111"));
        assertTrue(a.getDatos().contains("RECIBIDO"));
    }

    @Test
    void clasificaTecnicosYOfertas() {
        consumidor.recibir(new EventoTecniya("TecnicoRegistrado", LocalDateTime.now().toString(),
                Map.of("tecnicoId", 5, "usuarioId", 10, "nombre", "Luis Pérez")));
        consumidor.recibir(new EventoTecniya("OfertaAceptada", LocalDateTime.now().toString(),
                Map.of("solicitudId", 8, "ofertaId", 3, "tecnicoId", 5)));

        List<Auditoria> lista = guardadas();
        assertEquals(2, lista.size());
        assertTrue(lista.stream().anyMatch(a -> a.getEntidad().equals("TECNICO") && a.getEntidadId() == 5L));
        assertTrue(lista.stream().anyMatch(a -> a.getEntidad().equals("SOLICITUD") && a.getEntidadId() == 8L));
    }

    @Test
    void toleraEventosRarosSinFallar() {
        consumidor.recibir(new EventoTecniya(null, "fecha-invalida", null));

        Auditoria a = guardadas().get(0);
        assertEquals("Desconocido", a.getTipo());
        assertEquals("OTRO", a.getEntidad());
        assertNull(a.getEntidadId());
        assertEquals("{}", a.getDatos());
    }
}
