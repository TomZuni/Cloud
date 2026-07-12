package cl.education.enrollment.controller;

import cl.education.enrollment.dto.DispatchGuideQueueLogResponse;
import cl.education.enrollment.service.DispatchGuideQueueLogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint adicional exigido por el enunciado de la Sumativa 3: consulta los mensajes
 * que el consumidor ha ido tomando desde la cola 1 (dispatchGuideQueue) y guardando en
 * la tabla H2 "dispatch_guide_queue_log". Permite filtrar por estado (PROCESSED |
 * ERROR) para, entre otras cosas, evidenciar en el video que los mensajes con error
 * quedaron registrados antes de ser enviados a la DLQ.
 */
@RestController
@RequestMapping("/api/dispatch-guides/queue-log")
public class DispatchGuideQueueLogController {

    private final DispatchGuideQueueLogService queueLogService;

    public DispatchGuideQueueLogController(DispatchGuideQueueLogService queueLogService) {
        this.queueLogService = queueLogService;
    }

    @GetMapping
    public List<DispatchGuideQueueLogResponse> findAll(@RequestParam(required = false) String status) {
        return queueLogService.findAll(status);
    }
}
