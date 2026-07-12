package cl.education.enrollment.service;

import cl.education.enrollment.dto.QueueBindingRequest;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Service;

/**
 * Permite crear, listar y eliminar colas, exchanges y bindings de RabbitMQ en tiempo de
 * ejecucion mediante AmqpAdmin (tematica clave de la semana 8: administracion de recursos
 * RabbitMQ desde Java).
 */
@Service
public class GuideQueueAdminService {

    private final AmqpAdmin amqpAdmin;

    public GuideQueueAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void crearCola(String nombreCola) {
        amqpAdmin.declareQueue(new Queue(nombreCola, true));
    }

    public void crearExchange(String nombreExchange) {
        amqpAdmin.declareExchange(new DirectExchange(nombreExchange, true, false));
    }

    public void crearBinding(QueueBindingRequest request) {
        Binding binding = BindingBuilder.bind(new Queue(request.queueName()))
                .to(new DirectExchange(request.exchangeName()))
                .with(request.routingKey());
        amqpAdmin.declareBinding(binding);
    }

    public void eliminarCola(String nombreCola) {
        amqpAdmin.deleteQueue(nombreCola);
    }

    public void eliminarExchange(String nombreExchange) {
        amqpAdmin.deleteExchange(nombreExchange);
    }
}
