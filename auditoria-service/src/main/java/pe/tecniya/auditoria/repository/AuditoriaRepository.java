package pe.tecniya.auditoria.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.auditoria.domain.Auditoria;
import reactor.core.publisher.Flux;

public interface AuditoriaRepository extends ReactiveCrudRepository<Auditoria, Long> {

    Flux<Auditoria> findAllByOrderByIdDesc();

    Flux<Auditoria> findByTipoOrderByIdDesc(String tipo);
}
