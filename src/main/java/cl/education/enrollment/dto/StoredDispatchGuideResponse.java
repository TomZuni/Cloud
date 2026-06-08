package cl.education.enrollment.dto;

public record StoredDispatchGuideResponse(
        Long guideId,
        String bucketName,
        String key,
        String fileName,
        String contentType,
        long size
) {
}
