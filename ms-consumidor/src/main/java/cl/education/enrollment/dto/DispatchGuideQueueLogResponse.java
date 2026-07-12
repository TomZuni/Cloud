package cl.education.enrollment.dto;

import java.time.Instant;

public record DispatchGuideQueueLogResponse(
        Long id,
        Long guideId,
        String orderNumber,
        String carrierName,
        String status,
        String s3Key,
        String errorMessage,
        Instant processedAt
) {
}
