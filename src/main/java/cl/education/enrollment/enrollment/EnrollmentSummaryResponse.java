package cl.education.enrollment.enrollment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EnrollmentSummaryResponse(
        Long enrollmentId,
        String studentName,
        String studentEmail,
        List<EnrolledCourseResponse> courses,
        BigDecimal totalCost,
        Instant createdAt
) {
}
