package cl.education.enrollment.service;

import cl.education.enrollment.config.RabbitMQConfig;
import cl.education.enrollment.dto.DispatchGuideQueueMessage;
import cl.education.enrollment.model.DispatchGuide;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor: publica en dispatchGuideExchange cada vez que se crea una guia de despacho.
 * Es un componente independiente del consumidor (DispatchGuideQueueConsumer), tal como
 * exige el enunciado de la Sumativa 3.
 */
@Component
public class DispatchGuideQueueProducer {

    private final RabbitTemplate rabbitTemplate;

    public DispatchGuideQueueProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishGuideCreated(DispatchGuide guide) {
        DispatchGuideQueueMessage message = new DispatchGuideQueueMessage(
                guide.getId(),
                guide.getOrderNumber(),
                guide.getCarrierName()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.MAIN_ROUTING_KEY,
                message
        );
    }
}
