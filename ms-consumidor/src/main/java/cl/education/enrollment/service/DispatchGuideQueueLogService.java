package cl.education.enrollment.service;

import cl.education.enrollment.dto.DispatchGuideQueueLogResponse;
import cl.education.enrollment.model.DispatchGuideQueueLog;
import cl.education.enrollment.repository.DispatchGuideQueueLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint adicional exigido por el enunciado: expone lo que el consumidor ha ido
 * consumiendo desde la cola 1 (dispatchGuideQueue) y guardando en la tabla H2
 * "dispatch_guide_queue_log" (distinta a la tabla "dispatch_guides" del productor).
 */
@Service
public class DispatchGuideQueueLogService {

    private final DispatchGuideQueueLogRepository repository;

    public DispatchGuideQueueLogService(DispatchGuideQueueLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DispatchGuideQueueLogResponse> findAll(String status) {
        List<DispatchGuideQueueLog> logs = (status == null || status.isBlank())
                ? repository.findAllByOrderByProcessedAtDesc()
                : repository.findAllByStatusOrderByProcessedAtDesc(status.toUpperCase());

        return logs.stream().map(this::toResponse).toList();
    }

    private DispatchGuideQueueLogResponse toResponse(DispatchGuideQueueLog log) {
        return new DispatchGuideQueueLogResponse(
                log.getId(),
                log.getGuideId(),
                log.getOrderNumber(),
                log.getCarrierName(),
                log.getStatus(),
                log.getS3Key(),
                log.getErrorMessage(),
                log.getProcessedAt()
        );
    }
}
