package cl.education.enrollment.dto;

import java.io.Serializable;

/**
 * Mensaje publicado en la cola dispatchGuideQueue cada vez que se crea una guia de despacho.
 * Debe ser Serializable/convertible a JSON para que Jackson2JsonMessageConverter lo transporte.
 */
public record DispatchGuideQueueMessage(
        Long guideId,
        String orderNumber,
        String carrierName
) implements Serializable {
}
