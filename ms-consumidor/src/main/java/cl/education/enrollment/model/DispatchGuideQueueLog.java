package cl.education.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Tabla distinta a "dispatch_guides", usada exclusivamente por el consumidor de la cola
 * RabbitMQ para registrar el resultado del procesamiento asincrono de cada guia
 * (generacion de PDF + subida a S3), tal como exige el enunciado de la Sumativa 3.
 */
@Entity
@Table(name = "dispatch_guide_queue_log")
public class DispatchGuideQueueLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dispatch_guide_queue_log_seq")
    @SequenceGenerator(name = "dispatch_guide_queue_log_seq", sequenceName = "dispatch_guide_queue_log_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long guideId;

    @Column(nullable = false, length = 80)
    private String orderNumber;

    @Column(nullable = false, length = 120)
    private String carrierName;

    @Column(nullable = false, length = 20)
    private String status; // PROCESSED | ERROR

    @Column(length = 500)
    private String s3Key;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    protected DispatchGuideQueueLog() {
    }

    public static DispatchGuideQueueLog success(Long guideId, String orderNumber, String carrierName, String s3Key) {
        DispatchGuideQueueLog log = new DispatchGuideQueueLog();
        log.guideId = guideId;
        log.orderNumber = orderNumber;
        log.carrierName = carrierName;
        log.status = "PROCESSED";
        log.s3Key = s3Key;
        return log;
    }

    public static DispatchGuideQueueLog error(Long guideId, String orderNumber, String carrierName, String errorMessage) {
        DispatchGuideQueueLog log = new DispatchGuideQueueLog();
        log.guideId = guideId;
        log.orderNumber = orderNumber;
        log.carrierName = carrierName;
        log.status = "ERROR";
        log.errorMessage = errorMessage;
        return log;
    }

    @PrePersist
    void prePersist() {
        processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getGuideId() {
        return guideId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getStatus() {
        return status;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
