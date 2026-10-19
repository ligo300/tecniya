package pe.tecniya.tecnicos.eventos;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.tecniya.tecnicos.service.TecnicoService;

/** RN-20: escucha OrdenCalificada y actualiza el promedio del técnico. */
@Component
public class CalificacionConsumidor {

    private static final Logger log = LoggerFactory.getLogger(CalificacionConsumidor.class);

    private final TecnicoService tecnicos;

    public CalificacionConsumidor(TecnicoService tecnicos) {
        this.tecnicos = tecnicos;
    }

    @RabbitListener(queues = ColasConfig.COLA)
    public void alRecibir(EventoTecniya evento) {
        if (!"OrdenCalificada".equals(evento.tipo())) {
            return;
        }
        Map<String, Object> datos = evento.datos();
        long tecnicoId = ((Number) datos.get("tecnicoId")).longValue();
        int puntaje = ((Number) datos.get("puntaje")).intValue();
        // El listener corre en su propio hilo (no en uno de WebFlux), por eso se puede esperar.
        tecnicos.registrarCalificacion(tecnicoId, puntaje).block();
        log.info("Calificación {} registrada para el técnico {}", puntaje, tecnicoId);
    }
}
