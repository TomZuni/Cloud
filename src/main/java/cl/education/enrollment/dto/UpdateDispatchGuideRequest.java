package cl.education.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDispatchGuideRequest(
        @NotBlank
        @Size(max = 80)
        String orderNumber,

        @NotBlank
        @Size(max = 120)
        String carrierName,

        @NotBlank
        @Size(max = 30)
        String carrierRut,

        @NotBlank
        @Size(max = 120)
        String recipientName,

        @NotBlank
        @Size(max = 180)
        String originAddress,

        @NotBlank
        @Size(max = 180)
        String destinationAddress,

        @NotBlank
        @Size(max = 500)
        String packageDescription,

        @NotNull
        LocalDate dispatchDate
) {
}
