package cl.education.enrollment.enrollment;

import java.math.BigDecimal;

public record EnrolledCourseResponse(
        Long courseId,
        String name,
        BigDecimal cost
) {
}
