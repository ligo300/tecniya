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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import pe.tecniya.ordenes.domain.Calificacion;
import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.dto.CalificacionRequest;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ConflictoException;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.CalificacionRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** RN-15 y RN-20 con repositorios simulados. */
class CalificacionServiceTests {

    private final JwtUser cliente = new JwtUser(7L, "cliente@correo.com", "CLIENTE");

    private CalificacionRepository calificaciones;
    private AccesoOrden acceso;
    private EventoPublisher eventos;
    private CalificacionService servicio;

    @BeforeEach
    void preparar() {
        calificaciones = mock(CalificacionRepository.class);
        acceso = mock(AccesoOrden.class);
        eventos = mock(EventoPublisher.class);
        servicio = new CalificacionService(calificaciones, acceso, eventos);

        when(calificaciones.existsByOrdenId(1L)).thenReturn(Mono.just(false));
        when(calificaciones.save(any(Calificacion.class)))
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

    private void ordenEnEstado(EstadoOrden estado) {
        when(acceso.comoCliente(1L, cliente)).thenReturn(Mono.just(orden(estado)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void calificarUnaOrdenEntregadaPublicaElEventoParaElTecnico() {
        ordenEnEstado(EstadoOrden.ENTREGADO);

        StepVerifier.create(servicio.calificar(1L, new CalificacionRequest(5, "Excelente"), cliente))
                .assertNext(c -> assertEquals(5, c.puntaje()))
                .verifyComplete();

        ArgumentCaptor<Map<String, Object>> datos = ArgumentCaptor.forClass(Map.class);
        verify(eventos).publicar(eq("orden.calificada"), eq("OrdenCalificada"), datos.capture());
        assertEquals(100L, datos.getValue().get("tecnicoId"));
        assertEquals(5, datos.getValue().get("puntaje"));
    }

    @Test
    void soloSeCalificaUnaOrdenEntregada() {
        ordenEnEstado(EstadoOrden.LISTO);

        StepVerifier.create(servicio.calificar(1L, new CalificacionRequest(4, null), cliente))
                .expectError(ReglaNegocioException.class)
                .verify();
        verify(calificaciones, never()).save(any(Calificacion.class));
        verify(eventos, never()).publicar(anyString(), anyString(), anyMap());
    }

    @Test
    void noSeCalificaDosVeces() {
        ordenEnEstado(EstadoOrden.ENTREGADO);
        when(calificaciones.existsByOrdenId(1L)).thenReturn(Mono.just(true));

        StepVerifier.create(servicio.calificar(1L, new CalificacionRequest(4, null), cliente))
                .expectError(ConflictoException.class)
                .verify();
        verify(calificaciones, never()).save(any(Calificacion.class));
    }
}
