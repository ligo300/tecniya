package pe.tecniya.tecnicos.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.tecnicos.domain.EstadoTecnico;
import pe.tecniya.tecnicos.domain.Tecnico;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TecnicoRepository extends ReactiveCrudRepository<Tecnico, Long> {

    Mono<Tecnico> findByUsuarioId(Long usuarioId);

    Mono<Boolean> existsByUsuarioId(Long usuarioId);

    Mono<Boolean> existsByDocumento(String documento);

    Flux<Tecnico> findByEstadoOrderByCalifPromedioDesc(EstadoTecnico estado);

    Flux<Tecnico> findByEstado(EstadoTecnico estado);
}
