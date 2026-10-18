package pe.tecniya.solicitudes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.solicitudes.domain.EstadoSolicitud;
import pe.tecniya.solicitudes.domain.Solicitud;
import reactor.core.publisher.Flux;

public interface SolicitudRepository extends ReactiveCrudRepository<Solicitud, Long> {

    Flux<Solicitud> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    Flux<Solicitud> findByEstado(EstadoSolicitud estado);
}
