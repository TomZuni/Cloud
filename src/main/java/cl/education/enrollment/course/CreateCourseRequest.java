package cl.education.enrollment.course;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCourseRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(max = 120)
        String instructor,

        @NotNull
        @Positive
        Integer durationHours,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal cost
) {
}
