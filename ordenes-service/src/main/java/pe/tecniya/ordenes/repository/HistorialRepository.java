package pe.tecniya.ordenes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.ordenes.domain.OrdenHistorial;
import reactor.core.publisher.Flux;

public interface HistorialRepository extends ReactiveCrudRepository<OrdenHistorial, Long> {

    Flux<OrdenHistorial> findByOrdenIdOrderByIdAsc(Long ordenId);
}
