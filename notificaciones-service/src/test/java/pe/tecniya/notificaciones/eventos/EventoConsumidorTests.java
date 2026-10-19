package pe.tecniya.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.notificaciones.domain.Notificacion;
import pe.tecniya.notificaciones.repository.NotificacionRepository;

/** Qué notificaciones genera cada evento (sin RabbitMQ: se llama al consumidor directamente). */
@SpringBootTest
@ActiveProfiles("test")
class EventoConsumidorTests {

    @Autowired
    private EventoConsumidor consumidor;
    @Autowired
    private NotificacionRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
    }

    private EventoTecniya evento(String tipo, Map<String, Object> datos) {
        return new EventoTecniya(tipo, LocalDateTime.now().toString(), datos);
    }

    private List<Notificacion> guardadas() {
        return repositorio.findAll().collectList().block();
    }

    @Test
    void ordenCreadaAvisaAlClienteYAlTecnico() {
        consumidor.recibir(evento("OrdenCreada", Map.of("ordenId", 1, "codigo", "ORD-AAAA1111",
                "clienteId", 7, "tecnicoId", 100, "origen", "OFERTA", "precio", 90)));

        List<Notificacion> lista = guardadas();
        assertEquals(2, lista.size());
        assertTrue(lista.stream().anyMatch(n -> n.getDestinatarioTipo().equals("CLIENTE")
                && n.getDestinatarioId() == 7L && n.getMensaje().contains("ORD-AAAA1111")));
        assertTrue(lista.stream().anyMatch(n -> n.getDestinatarioTipo().equals("TECNICO")
                && n.getDestinatarioId() == 100L));
    }

    @Test
    void elCambioDeEstadoAvisaAlCliente() {
        consumidor.recibir(evento("OrdenCambioEstado", Map.of("ordenId", 1, "codigo", "ORD-AAAA1111",
                "clienteId", 7, "tecnicoId", 100, "estadoAnterior", "CONTRATADO",
                "estadoNuevo", "RECIBIDO", "usuarioId", 10)));

        List<Notificacion> lista = guardadas();
        assertEquals(1, lista.size());
        assertEquals("CLIENTE", lista.get(0).getDestinatarioTipo());
        assertTrue(lista.get(0).getMensaje().contains("RECIBIDO"));
    }

    @Test
    void elRegistroDeUnTecnicoAvisaAlTecnicoYAlAdministrador() {
        consumidor.recibir(evento("TecnicoRegistrado",
                Map.of("tecnicoId", 5, "usuarioId", 10, "nombre", "Luis Pérez")));

        List<Notificacion> lista = guardadas();
        assertEquals(2, lista.size());
        assertTrue(lista.stream().anyMatch(n -> n.getDestinatarioTipo().equals("ADMIN")
                && n.getMensaje().contains("Luis Pérez")));
    }

    @Test
    void laCalificacionAvisaAlTecnico() {
        consumidor.recibir(evento("OrdenCalificada", Map.of("ordenId", 1, "codigo", "ORD-AAAA1111",
                "clienteId", 7, "tecnicoId", 100, "puntaje", 5)));

        List<Notificacion> lista = guardadas();
        assertEquals(1, lista.size());
        assertEquals("TECNICO", lista.get(0).getDestinatarioTipo());
        assertTrue(lista.get(0).getMensaje().contains("5/5"));
    }

    @Test
    void ofertaAceptadaYPagoAvisanAlTecnico() {
        consumidor.recibir(evento("OfertaAceptada", Map.of("solicitudId", 1, "ofertaId", 2,
                "clienteId", 7, "tecnicoId", 100, "precio", 90)));
        consumidor.recibir(evento("PagoRegistrado", Map.of("ordenId", 1, "codigo", "ORD-AAAA1111",
                "clienteId", 7, "tecnicoId", 100, "monto", 90, "medio", "YAPE", "estado", "PAGADO")));

        assertEquals(2, guardadas().stream()
                .filter(n -> n.getDestinatarioTipo().equals("TECNICO")).count());
    }

    @Test
    void ignoraTiposDesconocidos() {
        consumidor.recibir(evento("AlgoNuevo", Map.of("x", 1)));
        assertEquals(0, guardadas().size());
    }

    @Test
    void unEventoIncompletoFallaParaQueRabbitLoReintente() {
        assertThrows(IllegalArgumentException.class,
                () -> consumidor.recibir(evento("OrdenCreada", Map.of("codigo", "X"))));
        assertEquals(0, guardadas().size());
    }
}
