package cl.education.enrollment.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DispatchGuideResponse(
        Long id,
        String orderNumber,
        String carrierName,
        String carrierRut,
        String recipientName,
        String originAddress,
        String destinationAddress,
        String packageDescription,
        LocalDate dispatchDate,
        String accessCode,
        String fileName,
        String localFilePath,
        String s3Key,
        boolean uploadedToS3,
        Instant createdAt,
        Instant updatedAt
) {
}
