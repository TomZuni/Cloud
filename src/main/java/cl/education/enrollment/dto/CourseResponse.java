package cl.education.enrollment.dto;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        String name,
        String instructor,
        Integer durationHours,
        BigDecimal cost
) {
}
