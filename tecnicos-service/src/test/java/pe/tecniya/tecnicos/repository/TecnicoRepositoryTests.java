package pe.tecniya.tecnicos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.Tecnico;
import pe.tecniya.tecnicos.domain.TipoEquipo;
import reactor.test.StepVerifier;

/** Pruebas de la capa de datos: insertar, actualizar, eliminar y listar. */
@SpringBootTest
@ActiveProfiles("test")
class TecnicoRepositoryTests {

    @Autowired
    private TecnicoRepository tecnicos;

    @Autowired
    private EspecialidadRepository especialidades;

    @BeforeEach
    void limpiar() {
        tecnicos.deleteAll().block();
    }

    private Tecnico nuevo(long usuarioId, String documento, EstadoTecnico estado) {
        Tecnico t = new Tecnico();
        t.setUsuarioId(usuarioId);
        t.setNombres("Luis");
        t.setApellidos("Quispe");
        t.setDocumento(documento);
        t.setTelefono("999111222");
        t.setZona("Lima Norte");
        t.setTarifaBase(new BigDecimal("50.00"));
        t.setEstado(estado);
        t.setCalifPromedio(BigDecimal.ZERO);
        t.setCalifCantidad(0);
        t.setCreadoEn(LocalDateTime.now());
        return t;
    }

    @Test
    void insertarAsignaIdYGuardaLosDatos() {
        StepVerifier.create(tecnicos.save(nuevo(1, "11111111", EstadoTecnico.PENDIENTE_VALIDACION)))
                .assertNext(t -> {
                    assertNotNull(t.getId());
                    assertEquals("Luis", t.getNombres());
                })
                .verifyComplete();
    }

    @Test
    void actualizarCambiaLosDatosGuardados() {
        Tecnico guardado = tecnicos.save(
                nuevo(2, "22222222", EstadoTecnico.PENDIENTE_VALIDACION)).block();
        guardado.setZona("Miraflores");
        guardado.setTarifaBase(new BigDecimal("80.00"));

        StepVerifier.create(tecnicos.save(guardado)
                        .then(tecnicos.findById(guardado.getId())))
                .assertNext(t -> {
                    assertEquals("Miraflores", t.getZona());
                    assertEquals(0, new BigDecimal("80.00").compareTo(t.getTarifaBase()));
                })
                .verifyComplete();
    }

    @Test
    void eliminarQuitaElRegistro() {
        Tecnico guardado = tecnicos.save(
                nuevo(3, "33333333", EstadoTecnico.ACTIVO)).block();

        StepVerifier.create(tecnicos.deleteById(guardado.getId())
                        .then(tecnicos.findById(guardado.getId())))
                .verifyComplete();
    }

    @Test
    void listarDevuelveTodosLosRegistros() {
        tecnicos.save(nuevo(4, "44444444", EstadoTecnico.ACTIVO)).block();
        tecnicos.save(nuevo(5, "55555555", EstadoTecnico.PENDIENTE_VALIDACION)).block();

        StepVerifier.create(tecnicos.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void listarPorEstadoSoloDevuelveLosActivos() {
        tecnicos.save(nuevo(6, "66666666", EstadoTecnico.ACTIVO)).block();
        tecnicos.save(nuevo(7, "77777777", EstadoTecnico.PENDIENTE_VALIDACION)).block();

        StepVerifier.create(tecnicos.findByEstadoOrderByCalifPromedioDesc(EstadoTecnico.ACTIVO))
                .assertNext(t -> assertEquals("66666666", t.getDocumento()))
                .verifyComplete();
    }

    @Test
    void documentoDuplicadoSeRechaza() {
        tecnicos.save(nuevo(8, "88888888", EstadoTecnico.ACTIVO)).block();

        StepVerifier.create(tecnicos.save(nuevo(9, "88888888", EstadoTecnico.ACTIVO)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void lasEspecialidadesSeGuardanYSeReemplazan() {
        Tecnico t = tecnicos.save(nuevo(10, "10101010", EstadoTecnico.ACTIVO)).block();

        especialidades.reemplazar(t.getId(), List.of(TipoEquipo.PC, TipoEquipo.TV)).block();
        StepVerifier.create(especialidades.listar(t.getId()).collectList())
                .assertNext(lista -> assertEquals(
                        List.of(TipoEquipo.PC, TipoEquipo.TV), lista))
                .verifyComplete();

        especialidades.reemplazar(t.getId(), List.of(TipoEquipo.CELULAR)).block();
        StepVerifier.create(especialidades.listar(t.getId()).collectList())
                .assertNext(lista -> assertEquals(List.of(TipoEquipo.CELULAR), lista))
                .verifyComplete();
    }
}
