package pe.tecniya.ordenes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.Pago;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagoRepository extends ReactiveCrudRepository<Pago, Long> {

    Flux<Pago> findByOrdenIdOrderByIdAsc(Long ordenId);

    Mono<Boolean> existsByNroOperacion(String nroOperacion);

    Mono<Boolean> existsByOrdenIdAndEstado(Long ordenId, EstadoPago estado);
}
