package pe.tecniya.notificaciones.eventos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.tecniya.notificaciones.service.NotificacionService;

@Component
public class EventoConsumidor {

    private static final Logger log = LoggerFactory.getLogger(EventoConsumidor.class);

    private final NotificacionService servicio;

    public EventoConsumidor(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @RabbitListener(queues = ColasConfig.COLA)
    public void recibir(EventoTecniya evento) {
        log.info("Evento recibido: {}", evento.tipo());
        // El listener corre en su propio hilo (no en uno de WebFlux), por eso se puede esperar.
        servicio.procesar(evento).block();
    }
}
