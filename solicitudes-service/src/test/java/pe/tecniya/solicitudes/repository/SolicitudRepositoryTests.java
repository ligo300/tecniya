package pe.tecniya.solicitudes.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.solicitudes.domain.EstadoOferta;
import pe.tecniya.solicitudes.domain.EstadoSolicitud;
import pe.tecniya.solicitudes.domain.Oferta;
import pe.tecniya.solicitudes.domain.Solicitud;
import pe.tecniya.solicitudes.domain.TipoEquipo;
import reactor.test.StepVerifier;

/** Pruebas de la capa de datos: insertar, actualizar, eliminar y listar. */
@SpringBootTest
@ActiveProfiles("test")
class SolicitudRepositoryTests {

    @Autowired
    private SolicitudRepository solicitudes;

    @Autowired
    private OfertaRepository ofertas;

    @BeforeEach
    void limpiar() {
        ofertas.deleteAll().then(solicitudes.deleteAll()).block();
    }

    private Solicitud nueva(long clienteId, TipoEquipo tipo, EstadoSolicitud estado) {
        Solicitud s = new Solicitud();
        s.setClienteId(clienteId);
        s.setTipoEquipo(tipo);
        s.setMarcaModelo("HP Pavilion");
        s.setDescripcionFalla("No enciende");
        s.setPresupuestoRef(new BigDecimal("100.00"));
        s.setEstado(estado);
        s.setCreadoEn(LocalDateTime.now());
        return s;
    }

    private Oferta nuevaOferta(long solicitudId, long tecnicoId) {
        Oferta o = new Oferta();
        o.setSolicitudId(solicitudId);
        o.setTecnicoId(tecnicoId);
        o.setPrecio(new BigDecimal("80.00"));
        o.setPlazoDias(3);
        o.setMensaje("Puedo revisarlo mañana");
        o.setEstado(EstadoOferta.PENDIENTE);
        o.setCreadoEn(LocalDateTime.now());
        return o;
    }

    @Test
    void insertarAsignaIdYGuardaLosDatos() {
        StepVerifier.create(solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)))
                .assertNext(s -> {
                    assertNotNull(s.getId());
                    assertEquals("No enciende", s.getDescripcionFalla());
                })
                .verifyComplete();
    }

    @Test
    void actualizarCambiaLosDatosGuardados() {
        Solicitud guardada = solicitudes.save(
                nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        guardada.setEstado(EstadoSolicitud.CANCELADA);
        guardada.setMarcaModelo("Dell XPS");

        StepVerifier.create(solicitudes.save(guardada)
                        .then(solicitudes.findById(guardada.getId())))
                .assertNext(s -> {
                    assertEquals(EstadoSolicitud.CANCELADA, s.getEstado());
                    assertEquals("Dell XPS", s.getMarcaModelo());
                })
                .verifyComplete();
    }

    @Test
    void eliminarQuitaLaSolicitudYSusOfertas() {
        Solicitud s = solicitudes.save(nueva(1, TipoEquipo.TV, EstadoSolicitud.ABIERTA)).block();
        ofertas.save(nuevaOferta(s.getId(), 7)).block();

        StepVerifier.create(solicitudes.deleteById(s.getId())
                        .then(solicitudes.findById(s.getId())))
                .verifyComplete();
        StepVerifier.create(ofertas.countBySolicitudId(s.getId()))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void listarDevuelveTodasLasSolicitudes() {
        solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        solicitudes.save(nueva(2, TipoEquipo.TV, EstadoSolicitud.ABIERTA)).block();

        StepVerifier.create(solicitudes.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void listarPorClienteSoloDevuelveLasSuyas() {
        solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        solicitudes.save(nueva(2, TipoEquipo.TV, EstadoSolicitud.ABIERTA)).block();

        StepVerifier.create(solicitudes.findByClienteIdOrderByCreadoEnDesc(1L))
                .assertNext(s -> assertEquals(1L, s.getClienteId()))
                .verifyComplete();
    }

    @Test
    void listarPorEstado() {
        solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        solicitudes.save(nueva(2, TipoEquipo.TV, EstadoSolicitud.CANCELADA)).block();

        StepVerifier.create(solicitudes.findByEstado(EstadoSolicitud.CANCELADA))
                .assertNext(s -> assertEquals(TipoEquipo.TV, s.getTipoEquipo()))
                .verifyComplete();
    }

    @Test
    void unTecnicoNoPuedeTenerDosOfertasEnLaMismaSolicitud() {
        Solicitud s = solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        ofertas.save(nuevaOferta(s.getId(), 7)).block();

        StepVerifier.create(ofertas.save(nuevaOferta(s.getId(), 7)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void lasOfertasSeListanDeMenorAMayorPrecio() {
        Solicitud s = solicitudes.save(nueva(1, TipoEquipo.PC, EstadoSolicitud.ABIERTA)).block();
        Oferta cara = nuevaOferta(s.getId(), 7);
        cara.setPrecio(new BigDecimal("150.00"));
        Oferta barata = nuevaOferta(s.getId(), 8);
        barata.setPrecio(new BigDecimal("60.00"));
        ofertas.save(cara).block();
        ofertas.save(barata).block();

        StepVerifier.create(ofertas.findBySolicitudIdOrderByPrecioAsc(s.getId()))
                .assertNext(o -> assertEquals(8L, o.getTecnicoId()))
                .assertNext(o -> assertEquals(7L, o.getTecnicoId()))
                .verifyComplete();
    }
}
