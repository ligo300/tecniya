package pe.tecniya.ordenes.eventos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Modo sin RabbitMQ (app.eventos.habilitado=false): guarda los eventos en memoria.
 * Se usa en las pruebas automáticas, que no necesitan un broker real.
 */
@Component
@ConditionalOnProperty(name = "app.eventos.habilitado", havingValue = "false")
public class EventoPublisherEnMemoria implements EventoPublisher {

    public record Publicado(String routingKey, EventoTecniya evento) {
    }

    private final List<Publicado> publicados = new CopyOnWriteArrayList<>();

    @Override
    public Mono<Void> publicar(String routingKey, String tipo, Map<String, Object> datos) {
        return Mono.fromRunnable(() -> publicados.add(new Publicado(routingKey,
                new EventoTecniya(tipo, java.time.LocalDateTime.now().toString(), datos))));
    }

    public List<Publicado> publicados() {
        return List.copyOf(publicados);
    }

    public List<String> claves() {
        return publicados.stream().map(Publicado::routingKey).toList();
    }

    public void limpiar() {
        publicados.clear();
    }
}
