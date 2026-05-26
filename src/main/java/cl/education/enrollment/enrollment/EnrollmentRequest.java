package cl.education.enrollment.enrollment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EnrollmentRequest(
        @NotBlank
        @Size(max = 120)
        String studentName,

        @NotBlank
        @Email
        @Size(max = 180)
        String studentEmail,

        @NotEmpty
        List<@Positive Long> courseIds
) {
}
