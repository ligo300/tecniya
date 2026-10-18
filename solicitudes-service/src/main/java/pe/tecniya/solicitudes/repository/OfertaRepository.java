package pe.tecniya.solicitudes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.solicitudes.domain.Oferta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OfertaRepository extends ReactiveCrudRepository<Oferta, Long> {

    Flux<Oferta> findBySolicitudIdOrderByPrecioAsc(Long solicitudId);

    Mono<Boolean> existsBySolicitudIdAndTecnicoId(Long solicitudId, Long tecnicoId);

    Mono<Long> countBySolicitudId(Long solicitudId);
}
