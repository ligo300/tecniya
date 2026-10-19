package pe.tecniya.ordenes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.ordenes.domain.Calificacion;
import reactor.core.publisher.Mono;

public interface CalificacionRepository extends ReactiveCrudRepository<Calificacion, Long> {

    Mono<Calificacion> findByOrdenId(Long ordenId);

    Mono<Boolean> existsByOrdenId(Long ordenId);
}
