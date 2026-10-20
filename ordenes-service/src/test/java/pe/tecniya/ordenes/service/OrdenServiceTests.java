package pe.tecniya.ordenes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.tecniya.ordenes.client.TecnicosClient;
import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.OrdenHistorial;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.CalificacionRepository;
import pe.tecniya.ordenes.repository.FotoRepository;
import pe.tecniya.ordenes.repository.HistorialRepository;
import pe.tecniya.ordenes.repository.OrdenRepository;
import pe.tecniya.ordenes.repository.PagoRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Máquina de estados de la orden (RN-11 a RN-14, RN-19) con repositorios simulados. */
class OrdenServiceTests {

    private static final String AUTORIZACION = "Bearer token-del-tecnico";

    private final JwtUser tecnicoUsuario = new JwtUser(10L, "tec@correo.com", "TECNICO");
    private final JwtUser clienteUsuario = new JwtUser(7L, "cliente@correo.com", "CLIENTE");

    private OrdenRepository ordenes;
    private HistorialRepository historial;
    private FotoRepository fotos;
    private PagoRepository pagos;
    private AccesoOrden acceso;
    private EventoPublisher eventos;
    private OrdenService servicio;

    @BeforeEach
    void preparar() {
        ordenes = mock(OrdenRepository.class);
        historial = mock(HistorialRepository.class);
        fotos = mock(FotoRepository.class);
        pagos = mock(PagoRepository.class);
        acceso = mock(AccesoOrden.class);
        eventos = mock(EventoPublisher.class);
        servicio = new OrdenService(ordenes, historial, fotos, pagos,
                mock(CalificacionRepository.class), mock(TecnicosClient.class), acceso, eventos);

        when(ordenes.save(any(Orden.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(historial.save(any(OrdenHistorial.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(eventos.publicar(anyString(), anyString(), anyMap())).thenReturn(Mono.empty());
    }

    private Orden orden(EstadoOrden estado) {
        Orden o = new Orden();
        o.setId(1L);
        o.setCodigo("ORD-AAAA1111");
        o.setClienteId(7L);
        o.setTecnicoId(100L);
        o.setPrecioAcordado(new BigDecimal("100.00"));
        o.setEstado(estado);
        return o;
    }

    private Orden ordenDelTecnico(EstadoOrden estado) {
        Orden o = orden(estado);
        when(acceso.comoTecnicoAsignado(1L, AUTORIZACION)).thenReturn(Mono.just(o));
        return o;
    }

    @Test
    void conFotoPasaAlSiguienteEstadoYDejaHistorialYEvento() {
        Orden o = ordenDelTecnico(EstadoOrden.CONTRATADO);
        when(fotos.existsByOrdenIdAndEstado(1L, EstadoOrden.RECIBIDO)).thenReturn(Mono.just(true));

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.RECIBIDO, tecnicoUsuario, AUTORIZACION))
                .assertNext(r -> assertEquals(EstadoOrden.RECIBIDO, r.estado()))
                .verifyComplete();

        assertEquals(EstadoOrden.RECIBIDO, o.getEstado());
        verify(historial).save(any(OrdenHistorial.class));
        verify(eventos).publicar(eq("orden.estado.cambiado"), eq("OrdenCambioEstado"), anyMap());
    }

    @Test
    void sinFotoNoCambiaDeEstado() {
        Orden o = ordenDelTecnico(EstadoOrden.CONTRATADO);
        when(fotos.existsByOrdenIdAndEstado(1L, EstadoOrden.RECIBIDO)).thenReturn(Mono.just(false));

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.RECIBIDO, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();

        assertEquals(EstadoOrden.CONTRATADO, o.getEstado());
        verify(ordenes, never()).save(any(Orden.class));
        verify(historial, never()).save(any(OrdenHistorial.class));
        verify(eventos, never()).publicar(anyString(), anyString(), anyMap());
    }

    @Test
    void noSePuedeSaltarUnEstado() {
        ordenDelTecnico(EstadoOrden.CONTRATADO);

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.LISTO, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    void noSePuedeRetrocederNiReabrirUnaOrdenEntregada() {
        ordenDelTecnico(EstadoOrden.ENTREGADO);

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.LISTO, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    void cancelarPorElCambioDeEstadoNoEstaPermitido() {
        ordenDelTecnico(EstadoOrden.CONTRATADO);

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.CANCELADO, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    void entregarExigeUnPagoConfirmado() {
        Orden o = ordenDelTecnico(EstadoOrden.LISTO);
        when(pagos.existsByOrdenIdAndEstado(1L, EstadoPago.PAGADO)).thenReturn(Mono.just(false));

        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.ENTREGADO, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();
        assertEquals(EstadoOrden.LISTO, o.getEstado());

        when(pagos.existsByOrdenIdAndEstado(1L, EstadoPago.PAGADO)).thenReturn(Mono.just(true));
        StepVerifier.create(servicio.cambiarEstado(1L, EstadoOrden.ENTREGADO, tecnicoUsuario, AUTORIZACION))
                .assertNext(r -> assertEquals(EstadoOrden.ENTREGADO, r.estado()))
                .verifyComplete();
    }

    @Test
    void elClienteSoloCancelaMientrasEstaContratada() {
        Orden o = orden(EstadoOrden.CONTRATADO);
        when(acceso.comoCliente(1L, clienteUsuario)).thenReturn(Mono.just(o));

        StepVerifier.create(servicio.cancelar(1L, clienteUsuario, "Bearer x"))
                .assertNext(r -> assertEquals(EstadoOrden.CANCELADO, r.estado()))
                .verifyComplete();
        verify(eventos).publicar(eq("orden.estado.cambiado"), eq("OrdenCambioEstado"), anyMap());

        Orden recibida = orden(EstadoOrden.RECIBIDO);
        when(acceso.comoCliente(1L, clienteUsuario)).thenReturn(Mono.just(recibida));
        StepVerifier.create(servicio.cancelar(1L, clienteUsuario, "Bearer x"))
                .expectError(ReglaNegocioException.class)
                .verify();
        assertEquals(EstadoOrden.RECIBIDO, recibida.getEstado());
    }

    @Test
    void elTecnicoCancelaHastaEnReparacionPeroNoDesdeListo() {
        Orden enReparacion = ordenDelTecnico(EstadoOrden.EN_REPARACION);
        StepVerifier.create(servicio.cancelar(1L, tecnicoUsuario, AUTORIZACION))
                .assertNext(r -> assertEquals(EstadoOrden.CANCELADO, r.estado()))
                .verifyComplete();
        assertEquals(EstadoOrden.CANCELADO, enReparacion.getEstado());

        ordenDelTecnico(EstadoOrden.LISTO);
        StepVerifier.create(servicio.cancelar(1L, tecnicoUsuario, AUTORIZACION))
                .expectError(ReglaNegocioException.class)
                .verify();
    }
}
