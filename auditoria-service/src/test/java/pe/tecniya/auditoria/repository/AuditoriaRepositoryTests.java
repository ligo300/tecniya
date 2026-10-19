package pe.tecniya.auditoria.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.auditoria.domain.Auditoria;
import reactor.test.StepVerifier;

/** Pruebas de la capa de datos: insertar, actualizar, eliminar y listar. */
@SpringBootTest
@ActiveProfiles("test")
class AuditoriaRepositoryTests {

    @Autowired
    private AuditoriaRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
    }

    private Auditoria nueva(String tipo, long ordenId) {
        Auditoria a = new Auditoria();
        a.setTipo(tipo);
        a.setEntidad("ORDEN");
        a.setEntidadId(ordenId);
        a.setDatos("{\"ordenId\":" + ordenId + "}");
        a.setOcurridoEn(LocalDateTime.now());
        a.setRegistradoEn(LocalDateTime.now());
        return a;
    }

    @Test
    void insertaYBusca() {
        Auditoria guardada = repositorio.save(nueva("OrdenCreada", 1)).block();
        assertNotNull(guardada.getId());

        StepVerifier.create(repositorio.findById(guardada.getId()))
                .assertNext(a -> assertEquals("OrdenCreada", a.getTipo()))
                .verifyComplete();
    }

    @Test
    void actualizaYElimina() {
        Auditoria a = repositorio.save(nueva("OrdenCreada", 1)).block();
        a.setTipo("OrdenCalificada");
        repositorio.save(a).block();

        StepVerifier.create(repositorio.findById(a.getId()))
                .assertNext(r -> assertEquals("OrdenCalificada", r.getTipo()))
                .verifyComplete();

        repositorio.deleteById(a.getId()).block();
        StepVerifier.create(repositorio.findById(a.getId())).verifyComplete();
    }

    @Test
    void listaDeLaMasNuevaALaMasAntiguaYFiltraPorTipo() {
        repositorio.save(nueva("OrdenCreada", 1)).block();
        repositorio.save(nueva("PagoRegistrado", 1)).block();
        repositorio.save(nueva("OrdenCreada", 2)).block();

        StepVerifier.create(repositorio.findAllByOrderByIdDesc())
                .assertNext(a -> assertEquals(2L, a.getEntidadId()))
                .assertNext(a -> assertEquals("PagoRegistrado", a.getTipo()))
                .assertNext(a -> assertEquals(1L, a.getEntidadId()))
                .verifyComplete();
        StepVerifier.create(repositorio.findByTipoOrderByIdDesc("OrdenCreada"))
                .expectNextCount(2).verifyComplete();
    }
}
