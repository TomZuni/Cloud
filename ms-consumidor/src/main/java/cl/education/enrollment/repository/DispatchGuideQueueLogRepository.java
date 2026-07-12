package cl.education.enrollment.repository;

import cl.education.enrollment.model.DispatchGuideQueueLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchGuideQueueLogRepository extends JpaRepository<DispatchGuideQueueLog, Long> {

    List<DispatchGuideQueueLog> findAllByStatusOrderByProcessedAtDesc(String status);

    List<DispatchGuideQueueLog> findAllByOrderByProcessedAtDesc();
}
