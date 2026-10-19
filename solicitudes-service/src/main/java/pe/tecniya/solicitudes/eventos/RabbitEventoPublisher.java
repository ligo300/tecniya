package pe.tecniya.solicitudes.eventos;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@ConditionalOnProperty(name = "app.eventos.habilitado", havingValue = "true", matchIfMissing = true)
public class RabbitEventoPublisher implements EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventoPublisher.class);

    private final RabbitTemplate rabbit;

    public RabbitEventoPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public Mono<Void> publicar(String routingKey, String tipo, Map<String, Object> datos) {
        EventoTecniya evento = new EventoTecniya(tipo, LocalDateTime.now().toString(), datos);
        // RabbitTemplate es bloqueante: se ejecuta fuera de los hilos de WebFlux.
        return Mono.fromRunnable(() -> rabbit.convertAndSend(RabbitConfig.EXCHANGE, routingKey, evento))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("Evento {} publicado ({})", tipo, routingKey))
                .onErrorResume(e -> {
                    log.warn("No se pudo publicar {}: {}", tipo, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
