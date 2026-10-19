package pe.tecniya.ordenes.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.ordenes.domain.Calificacion;
import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.MedioPago;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.OrdenHistorial;
import pe.tecniya.ordenes.domain.OrigenOrden;
import pe.tecniya.ordenes.domain.Pago;
import pe.tecniya.ordenes.domain.TipoEquipo;
import reactor.test.StepVerifier;

/** Pruebas de la capa de datos: insertar, actualizar, eliminar, listar y restricciones. */
@SpringBootTest
@ActiveProfiles("test")
class OrdenRepositoryTests {

    @Autowired
    private OrdenRepository ordenes;
    @Autowired
    private HistorialRepository historial;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private CalificacionRepository calificaciones;

    @BeforeEach
    void limpiar() {
        calificaciones.deleteAll()
                .then(pagos.deleteAll())
                .then(historial.deleteAll())
                .then(ordenes.deleteAll())
                .block();
    }

    private Orden nueva(long clienteId, long tecnicoId, Long ofertaId) {
        Orden o = new Orden();
        o.setCodigo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        o.setClienteId(clienteId);
        o.setTecnicoId(tecnicoId);
        o.setOfertaId(ofertaId);
        o.setOrigen(ofertaId == null ? OrigenOrden.DIRECTA : OrigenOrden.OFERTA);
        o.setTipoEquipo(TipoEquipo.PC);
        o.setDescripcion("No enciende");
        o.setPrecioAcordado(new BigDecimal("80.00"));
        o.setEstado(EstadoOrden.CONTRATADO);
        o.setCreadoEn(LocalDateTime.now());
        o.setActualizadoEn(LocalDateTime.now());
        return o;
    }

    private Pago pago(long ordenId, String nro) {
        Pago p = new Pago();
        p.setOrdenId(ordenId);
        p.setMonto(new BigDecimal("80.00"));
        p.setMedio(MedioPago.YAPE);
        p.setNroOperacion(nro);
        p.setEstado(EstadoPago.PAGADO);
        p.setFecha(LocalDateTime.now());
        return p;
    }

    @Test
    void insertaYBuscaUnaOrden() {
        Orden guardada = ordenes.save(nueva(1, 10, 500L)).block();
        assertNotNull(guardada.getId());

        StepVerifier.create(ordenes.findById(guardada.getId()))
                .assertNext(o -> {
                    assertEquals(EstadoOrden.CONTRATADO, o.getEstado());
                    assertEquals(0, new BigDecimal("80.00").compareTo(o.getPrecioAcordado()));
                })
                .verifyComplete();
    }

    @Test
    void actualizaElEstado() {
        Orden o = ordenes.save(nueva(1, 10, null)).block();
        o.setEstado(EstadoOrden.RECIBIDO);

        StepVerifier.create(ordenes.save(o).then(ordenes.findById(o.getId())))
                .assertNext(r -> assertEquals(EstadoOrden.RECIBIDO, r.getEstado()))
                .verifyComplete();
    }

    @Test
    void eliminarUnaOrdenBorraSuHistorial() {
        Orden o = ordenes.save(nueva(1, 10, null)).block();
        OrdenHistorial h = new OrdenHistorial();
        h.setOrdenId(o.getId());
        h.setEstadoNuevo(EstadoOrden.CONTRATADO);
        h.setUsuarioId(1L);
        h.setFecha(LocalDateTime.now());
        historial.save(h).block();

        ordenes.deleteById(o.getId()).block();

        StepVerifier.create(historial.findByOrdenIdOrderByIdAsc(o.getId()))
                .verifyComplete();
    }

    @Test
    void listaPorClienteYPorTecnico() {
        ordenes.save(nueva(1, 10, null)).block();
        ordenes.save(nueva(1, 11, null)).block();
        ordenes.save(nueva(2, 10, null)).block();

        StepVerifier.create(ordenes.findByClienteIdOrderByCreadoEnDesc(1L))
                .expectNextCount(2).verifyComplete();
        StepVerifier.create(ordenes.findByTecnicoIdOrderByCreadoEnDesc(10L))
                .expectNextCount(2).verifyComplete();
        StepVerifier.create(ordenes.findAllByOrderByCreadoEnDesc())
                .expectNextCount(3).verifyComplete();
    }

    @Test
    void buscaPorOfertaId() {
        ordenes.save(nueva(1, 10, 700L)).block();

        StepVerifier.create(ordenes.findByOfertaId(700L))
                .assertNext(o -> assertEquals(1L, o.getClienteId()))
                .verifyComplete();
        StepVerifier.create(ordenes.findByOfertaId(701L)).verifyComplete();
    }

    @Test
    void noPermiteDosOrdenesDeLaMismaOferta() {
        ordenes.save(nueva(1, 10, 800L)).block();

        StepVerifier.create(ordenes.save(nueva(2, 11, 800L)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void noPermiteRepetirElNumeroDeOperacion() {
        Orden a = ordenes.save(nueva(1, 10, null)).block();
        Orden b = ordenes.save(nueva(2, 11, null)).block();
        pagos.save(pago(a.getId(), "OP-1")).block();

        StepVerifier.create(pagos.save(pago(b.getId(), "OP-1")))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        StepVerifier.create(pagos.existsByNroOperacion("OP-1"))
                .expectNext(true).verifyComplete();
    }

    @Test
    void unaOrdenSoloTieneUnaCalificacion() {
        Orden o = ordenes.save(nueva(1, 10, null)).block();
        Calificacion c = new Calificacion();
        c.setOrdenId(o.getId());
        c.setPuntaje(5);
        c.setFecha(LocalDateTime.now());
        calificaciones.save(c).block();

        Calificacion otra = new Calificacion();
        otra.setOrdenId(o.getId());
        otra.setPuntaje(3);
        otra.setFecha(LocalDateTime.now());

        StepVerifier.create(calificaciones.save(otra))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        StepVerifier.create(calificaciones.existsByOrdenId(o.getId()))
                .expectNext(true).verifyComplete();
    }
}
