package pe.tecniya.tecnicos.eventos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cola propia de este servicio y las routing keys a las que se suscribe. */
@Configuration
public class ColasConfig {

    public static final String COLA = "tecnicos.calificaciones";

    private static final List<String> CLAVES = List.of("orden.calificada");

    @Bean
    Declarables colaYEnlaces(TopicExchange tecniyaEventos) {
        Queue cola = new Queue(COLA, true);
        List<Declarable> declaraciones = new ArrayList<>();
        declaraciones.add(cola);
        for (String clave : CLAVES) {
            declaraciones.add(BindingBuilder.bind(cola).to(tecniyaEventos).with(clave));
        }
        return new Declarables(declaraciones);
    }
}
