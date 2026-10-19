package pe.tecniya.notificaciones.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.notificaciones.domain.Notificacion;
import reactor.test.StepVerifier;

/** Pruebas de la capa de datos: insertar, actualizar, eliminar y listar. */
@SpringBootTest
@ActiveProfiles("test")
class NotificacionRepositoryTests {

    @Autowired
    private NotificacionRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
    }

    private Notificacion nueva(String tipo, long destinatario, String mensaje) {
        Notificacion n = new Notificacion();
        n.setDestinatarioTipo(tipo);
        n.setDestinatarioId(destinatario);
        n.setEvento("OrdenCreada");
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setCreadaEn(LocalDateTime.now());
        return n;
    }

    @Test
    void insertaYBusca() {
        Notificacion guardada = repositorio.save(nueva("CLIENTE", 1, "Hola")).block();
        assertNotNull(guardada.getId());

        StepVerifier.create(repositorio.findById(guardada.getId()))
                .assertNext(n -> assertEquals("Hola", n.getMensaje()))
                .verifyComplete();
    }

    @Test
    void marcaComoLeida() {
        Notificacion n = repositorio.save(nueva("CLIENTE", 1, "Hola")).block();
        n.setLeida(true);

        StepVerifier.create(repositorio.save(n).then(repositorio.findById(n.getId())))
                .assertNext(r -> assertTrue(r.isLeida()))
                .verifyComplete();
    }

    @Test
    void eliminaUnaNotificacion() {
        Notificacion n = repositorio.save(nueva("CLIENTE", 1, "Hola")).block();
        repositorio.deleteById(n.getId()).block();

        StepVerifier.create(repositorio.findById(n.getId())).verifyComplete();
    }

    @Test
    void listaPorDestinatarioDeLaMasNuevaALaMasAntigua() {
        repositorio.save(nueva("CLIENTE", 1, "primera")).block();
        repositorio.save(nueva("CLIENTE", 1, "segunda")).block();
        repositorio.save(nueva("CLIENTE", 2, "ajena")).block();
        repositorio.save(nueva("TECNICO", 1, "de otro tipo")).block();

        StepVerifier.create(repositorio.findByDestinatarioTipoAndDestinatarioIdOrderByIdDesc("CLIENTE", 1L))
                .assertNext(n -> assertEquals("segunda", n.getMensaje()))
                .assertNext(n -> assertEquals("primera", n.getMensaje()))
                .verifyComplete();
    }

    @Test
    void filtraSoloNoLeidas() {
        Notificacion leida = repositorio.save(nueva("CLIENTE", 1, "leída")).block();
        leida.setLeida(true);
        repositorio.save(leida).block();
        repositorio.save(nueva("CLIENTE", 1, "pendiente")).block();

        StepVerifier.create(repositorio
                .findByDestinatarioTipoAndDestinatarioIdAndLeidaOrderByIdDesc("CLIENTE", 1L, false))
                .assertNext(n -> assertEquals("pendiente", n.getMensaje()))
                .verifyComplete();
    }
}
