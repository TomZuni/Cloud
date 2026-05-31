package cl.education.enrollment.dto;

import java.math.BigDecimal;

public record EnrolledCourseResponse(
        Long courseId,
        String name,
        BigDecimal cost
) {
}
