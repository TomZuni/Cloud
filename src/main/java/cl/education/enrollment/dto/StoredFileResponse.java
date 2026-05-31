package cl.education.enrollment.dto;

public record StoredFileResponse(
        Long enrollmentId,
        String bucketName,
        String key,
        String fileName,
        String contentType,
        long size
) {
}
