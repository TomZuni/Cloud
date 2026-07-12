package cl.education.enrollment.dto;

public record QueueBindingRequest(
        String queueName,
        String exchangeName,
        String routingKey
) {
}
