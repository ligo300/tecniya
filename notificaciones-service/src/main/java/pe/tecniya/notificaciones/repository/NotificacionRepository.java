package pe.tecniya.notificaciones.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import pe.tecniya.notificaciones.domain.Notificacion;
import reactor.core.publisher.Flux;

public interface NotificacionRepository extends ReactiveCrudRepository<Notificacion, Long> {

    Flux<Notificacion> findByDestinatarioTipoAndDestinatarioIdOrderByIdDesc(String tipo, Long id);

    Flux<Notificacion> findByDestinatarioTipoAndDestinatarioIdAndLeidaOrderByIdDesc(
            String tipo, Long id, boolean leida);
}
