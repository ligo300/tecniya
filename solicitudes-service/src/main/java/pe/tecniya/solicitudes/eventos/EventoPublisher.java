package pe.tecniya.solicitudes.eventos;

import java.util.Map;

import reactor.core.publisher.Mono;

/** Publica eventos de negocio. Un fallo al publicar nunca debe romper la operación del usuario. */
public interface EventoPublisher {

    Mono<Void> publicar(String routingKey, String tipo, Map<String, Object> datos);
}
