package cl.education.enrollment.course;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        String name,
        String instructor,
        Integer durationHours,
        BigDecimal cost
) {
}
