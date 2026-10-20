package pe.tecniya.ordenes.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.MedioPago;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.Pago;
import pe.tecniya.ordenes.dto.PagoRequest;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ConflictoException;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.PagoRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Reglas del pago (RN-17 a RN-19) probadas con repositorios simulados, sin Spring ni base de datos. */
class PagoServiceTests {

    private final JwtUser cliente = new JwtUser(7L, "cliente@correo.com", "CLIENTE");

    private PagoRepository pagos;
    private AccesoOrden acceso;
    private EventoPublisher eventos;
    private PagoService servicio;

    @BeforeEach
    void preparar() {
        pagos = mock(PagoRepository.class);
        acceso = mock(AccesoOrden.class);
        eventos = mock(EventoPublisher.class);
        servicio = new PagoService(pagos, acceso, eventos);

        when(pagos.existsByOrdenIdAndEstado(1L, EstadoPago.PAGADO)).thenReturn(Mono.just(false));
        when(pagos.existsByNroOperacion(anyString())).thenReturn(Mono.just(false));
        when(pagos.save(any(Pago.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
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

    private void ordenEnEstado(EstadoOrden estado) {
        when(acceso.comoCliente(1L, cliente)).thenReturn(Mono.just(orden(estado)));
    }

    private PagoRequest pago(String monto, String nro) {
        return new PagoRequest(new BigDecimal(monto), MedioPago.YAPE, nro);
    }

    @Test
    void unPagoValidoQuedaPagadoYPublicaElEvento() {
        ordenEnEstado(EstadoOrden.LISTO);

        StepVerifier.create(servicio.pagar(1L, pago("100.00", "OP-1"), cliente))
                .assertNext(p -> assertEquals(EstadoPago.PAGADO, p.estado()))
                .verifyComplete();
        verify(eventos).publicar(eq("pago.registrado"), eq("PagoRegistrado"), anyMap());
    }

    @Test
    void unNumeroQueEmpiezaConRechazoQuedaRechazado() {
        ordenEnEstado(EstadoOrden.LISTO);

        StepVerifier.create(servicio.pagar(1L, pago("100.00", "rechazo-9"), cliente))
                .assertNext(p -> assertEquals(EstadoPago.RECHAZADO, p.estado()))
                .verifyComplete();
    }

    @Test
    void soloSePuedePagarCuandoLaOrdenEstaListo() {
        ordenEnEstado(EstadoOrden.EN_REPARACION);

        StepVerifier.create(servicio.pagar(1L, pago("100.00", "OP-1"), cliente))
                .expectError(ReglaNegocioException.class)
                .verify();
        verify(pagos, never()).save(any(Pago.class));
        verify(eventos, never()).publicar(anyString(), anyString(), anyMap());
    }

    @Test
    void elMontoNoPuedeSuperarElPrecioAcordado() {
        ordenEnEstado(EstadoOrden.LISTO);

        StepVerifier.create(servicio.pagar(1L, pago("100.01", "OP-1"), cliente))
                .expectError(ReglaNegocioException.class)
                .verify();
        verify(pagos, never()).save(any(Pago.class));
    }

    @Test
    void unPagoParcialEsValido() {
        ordenEnEstado(EstadoOrden.LISTO);

        StepVerifier.create(servicio.pagar(1L, pago("40.00", "OP-2"), cliente))
                .assertNext(p -> assertEquals(0, new BigDecimal("40.00").compareTo(p.monto())))
                .verifyComplete();
    }

    @Test
    void noSePagaDosVecesUnaOrdenYaPagada() {
        ordenEnEstado(EstadoOrden.LISTO);
        when(pagos.existsByOrdenIdAndEstado(1L, EstadoPago.PAGADO)).thenReturn(Mono.just(true));

        StepVerifier.create(servicio.pagar(1L, pago("100.00", "OP-3"), cliente))
                .expectError(ConflictoException.class)
                .verify();
        verify(pagos, never()).save(any(Pago.class));
    }

    @Test
    void elNumeroDeOperacionNoSeRepite() {
        ordenEnEstado(EstadoOrden.LISTO);
        when(pagos.existsByNroOperacion("OP-4")).thenReturn(Mono.just(true));

        StepVerifier.create(servicio.pagar(1L, pago("100.00", "OP-4"), cliente))
                .expectError(ConflictoException.class)
                .verify();
    }
}
