package cl.education.enrollment.service;

import cl.education.enrollment.config.RabbitMQConfig;
import cl.education.enrollment.dto.DispatchGuideQueueMessage;
import cl.education.enrollment.model.DispatchGuide;
import cl.education.enrollment.model.DispatchGuideQueueLog;
import cl.education.enrollment.repository.DispatchGuideQueueLogRepository;
import cl.education.enrollment.repository.DispatchGuideRepository;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Consumidor: componente distinto al productor (DispatchGuideQueueProducer). Escucha
 * dispatchGuideQueue con acknowledgment manual. Si el procesamiento es correcto, genera el
 * PDF, lo sube a S3 y guarda un registro en la tabla "dispatch_guide_queue_log" (tabla
 * distinta a "dispatch_guides"). Si falla (incluyendo el error forzado de prueba), rechaza
 * el mensaje (nack sin requeue) y RabbitMQ lo enruta automaticamente a la DLQ
 * (dispatchGuideErrorQueue) gracias al Dead Letter Exchange configurado en la cola.
 *
 * Para forzar un error y demostrar la DLQ en el video: basta con crear una guia cuyo
 * "orderNumber" contenga el texto "ERROR_TEST" (sin distinguir mayusculas/minusculas).
 */
@Component
public class DispatchGuideQueueConsumer {

    private static final String CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DispatchGuideRepository dispatchGuideRepository;
    private final DispatchGuideQueueLogRepository queueLogRepository;
    private final DispatchGuidePdfService pdfService;
    private final S3Client s3Client;
    private final String bucketName;
    private final String s3Prefix;

    public DispatchGuideQueueConsumer(
            DispatchGuideRepository dispatchGuideRepository,
            DispatchGuideQueueLogRepository queueLogRepository,
            DispatchGuidePdfService pdfService,
            S3Client s3Client,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${dispatch-guides.s3-prefix}") String s3Prefix
    ) {
        this.dispatchGuideRepository = dispatchGuideRepository;
        this.queueLogRepository = queueLogRepository;
        this.pdfService = pdfService;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.s3Prefix = s3Prefix;
    }

    @RabbitListener(id = "listener-dispatchGuideQueue", queues = RabbitMQConfig.MAIN_QUEUE, ackMode = "MANUAL")
    @Transactional
    public void onGuideCreated(
            @Payload DispatchGuideQueueMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            if (message.orderNumber() != null && message.orderNumber().toUpperCase().contains("ERROR_TEST")) {
                throw new IllegalStateException(
                        "Error forzado de prueba (ERROR_TEST) para demostrar el enrutamiento a la DLQ.");
            }

            DispatchGuide guide = dispatchGuideRepository.findById(message.guideId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe la guia de despacho con id: " + message.guideId()));

            byte[] content = pdfService.generate(guide);
            String key = s3Key(guide);

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(CONTENT_TYPE)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));

            guide.markUploaded(key);
            dispatchGuideRepository.save(guide);

            queueLogRepository.save(DispatchGuideQueueLog.success(
                    guide.getId(), guide.getOrderNumber(), guide.getCarrierName(), key));

            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            queueLogRepository.save(DispatchGuideQueueLog.error(
                    message.guideId(), message.orderNumber(), message.carrierName(), exception.getMessage()));

            // requeue = false -> el mensaje NO vuelve a dispatchGuideQueue, se enruta a la DLQ
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String s3Key(DispatchGuide guide) {
        String cleanPrefix = s3Prefix.replaceAll("^/+|/+$", "");
        String relativeKey = DATE_PATH_FORMAT.format(guide.getDispatchDate())
                + "/" + slug(guide.getCarrierName())
                + "/guia-despacho-" + guide.getId() + ".pdf";
        return cleanPrefix.isBlank() ? relativeKey : cleanPrefix + "/" + relativeKey;
    }

    private String slug(String text) {
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "transportista" : normalized;
    }
}
