package cl.education.enrollment.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de colas RabbitMQ para el procesamiento asincrono de guias de despacho.
 *
 * Cola principal (dispatchGuideQueue): recibe un mensaje cada vez que se crea una guia de
 * despacho. Un componente consumidor distinto la procesa: genera el PDF, lo sube a S3 y
 * registra el resultado en una tabla H2 distinta a la usada por el resto del sistema.
 *
 * Cola de errores / DLQ (dispatchGuideErrorQueue): recibe automaticamente cualquier mensaje
 * que el consumidor rechace (nack) mediante el mecanismo de Dead Letter Exchange (DLX) de
 * RabbitMQ, sin necesidad de reenviarlo manualmente desde el codigo.
 */
@Configuration
public class RabbitMQConfig {

    public static final String MAIN_QUEUE = "dispatchGuideQueue";
    public static final String DLX_QUEUE = "dispatchGuideErrorQueue";

    public static final String MAIN_EXCHANGE = "dispatchGuideExchange";
    public static final String DLX_EXCHANGE = "dispatchGuideErrorExchange";

    public static final String MAIN_ROUTING_KEY = "dispatch.guide.created";
    public static final String DLX_ROUTING_KEY = "dispatch.guide.error";

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    Queue dispatchGuideQueue() {
        // x-dead-letter-exchange: si el consumidor hace nack, RabbitMQ reenvia
        // automaticamente el mensaje a la DLQ a traves de este exchange.
        return new Queue(MAIN_QUEUE, true, false, false,
                Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", DLX_ROUTING_KEY
                ));
    }

    @Bean
    Queue dispatchGuideErrorQueue() {
        return new Queue(DLX_QUEUE, true);
    }

    @Bean
    DirectExchange dispatchGuideExchange() {
        return new DirectExchange(MAIN_EXCHANGE);
    }

    @Bean
    DirectExchange dispatchGuideErrorExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    Binding dispatchGuideBinding(Queue dispatchGuideQueue, DirectExchange dispatchGuideExchange) {
        return BindingBuilder.bind(dispatchGuideQueue)
                .to(dispatchGuideExchange)
                .with(MAIN_ROUTING_KEY);
    }

    @Bean
    Binding dispatchGuideErrorBinding(Queue dispatchGuideErrorQueue, DirectExchange dispatchGuideErrorExchange) {
        return BindingBuilder.bind(dispatchGuideErrorQueue)
                .to(dispatchGuideErrorExchange)
                .with(DLX_ROUTING_KEY);
    }
}
