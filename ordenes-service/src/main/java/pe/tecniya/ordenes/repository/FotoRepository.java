package pe.tecniya.ordenes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.OrdenFoto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FotoRepository extends ReactiveCrudRepository<OrdenFoto, Long> {

    Flux<OrdenFoto> findByOrdenIdOrderByIdAsc(Long ordenId);

    Mono<Boolean> existsByOrdenIdAndEstado(Long ordenId, EstadoOrden estado);
}
