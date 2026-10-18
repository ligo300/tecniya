package pe.tecniya.solicitudes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.tecniya.solicitudes.domain.Oferta;
import pe.tecniya.solicitudes.domain.Solicitud;
import reactor.core.publisher.Mono;

/**
 * Versión temporal: ordenes-service todavía no existe. En el paso 9 se reemplaza
 * por una implementación con WebClient que crea la orden de verdad.
 */
@Component
public class OrdenesClientSimulado implements OrdenesClient {

    private static final Logger log = LoggerFactory.getLogger(OrdenesClientSimulado.class);

    @Override
    public Mono<Void> crearOrdenDesdeOferta(Solicitud solicitud, Oferta oferta) {
        return Mono.fromRunnable(() -> log.info(
                "[SIMULADO] Se crearía una orden: solicitud={}, oferta={}, tecnico={}, precio={}",
                solicitud.getId(), oferta.getId(), oferta.getTecnicoId(), oferta.getPrecio()));
    }
}
