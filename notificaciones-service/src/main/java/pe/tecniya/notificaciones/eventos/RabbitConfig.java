package pe.tecniya.notificaciones.eventos;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exchange compartido por todos los servicios y conversión de mensajes a JSON. */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "tecniya.eventos";

    @Bean
    TopicExchange tecniyaEventos() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Los mensajes viajan como JSON; cada servicio los lee con su propia clase EventoTecniya. */
    @Bean
    MessageConverter conversorJson() {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper tipos = new DefaultJackson2JavaTypeMapper();
        tipos.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        tipos.setTrustedPackages("pe.tecniya.*");
        conversor.setJavaTypeMapper(tipos);
        return conversor;
    }
}
