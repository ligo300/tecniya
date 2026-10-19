package pe.tecniya.ordenes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.ordenes.domain.Orden;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrdenRepository extends ReactiveCrudRepository<Orden, Long> {

    Flux<Orden> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    Flux<Orden> findByTecnicoIdOrderByCreadoEnDesc(Long tecnicoId);

    Flux<Orden> findAllByOrderByCreadoEnDesc();

    Mono<Orden> findByOfertaId(Long ofertaId);
}
