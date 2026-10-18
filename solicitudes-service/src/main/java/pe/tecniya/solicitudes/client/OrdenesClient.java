package pe.tecniya.solicitudes.client;

import pe.tecniya.solicitudes.domain.Oferta;
import pe.tecniya.solicitudes.domain.Solicitud;
import reactor.core.publisher.Mono;

/** Llamada síncrona a ordenes-service para crear la orden al aceptar una oferta (RN-08). */
public interface OrdenesClient {

    Mono<Void> crearOrdenDesdeOferta(Solicitud solicitud, Oferta oferta);
}
