package cl.education.enrollment.service;

import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.dto.StoredFileResponse;
import cl.education.enrollment.exception.BusinessRuleException;
import cl.education.enrollment.exception.ResourceNotFoundException;
import cl.education.enrollment.exception.StorageOperationException;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class EnrollmentSummaryStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3Client;
    private final EnrollmentSummaryFileService summaryFileService;
    private final String bucketName;
    private final String summaryPrefix;

    public EnrollmentSummaryStorageService(
            S3Client s3Client,
            EnrollmentSummaryFileService summaryFileService,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.summary-prefix}") String summaryPrefix
    ) {
        this.s3Client = s3Client;
        this.summaryFileService = summaryFileService;
        this.bucketName = bucketName;
        this.summaryPrefix = summaryPrefix;
    }

    public StoredFileResponse uploadGeneratedSummary(Long enrollmentId) {
        GeneratedFile file = summaryFileService.generate(enrollmentId);
        return putSummary(enrollmentId, file.fileName(), file.contentType(), file.content());
    }

    public StoredFileResponse replaceSummary(Long enrollmentId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("El archivo de reemplazo no puede estar vacio.");
        }

        String key = summaryKey(enrollmentId);
        ensureSummaryExists(key, enrollmentId);

        try {
            return putSummary(
                    enrollmentId,
                    summaryFileService.fileName(enrollmentId),
                    contentType(file),
                    file.getBytes()
            );
        } catch (IOException exception) {
            throw new StorageOperationException("No fue posible leer el archivo recibido.", exception);
        }
    }

    public GeneratedFile downloadSummary(Long enrollmentId) {
        String key = summaryKey(enrollmentId);
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = DEFAULT_CONTENT_TYPE;
            }

            return new GeneratedFile(
                    summaryFileService.fileName(enrollmentId),
                    contentType,
                    response.asByteArray()
            );
        } catch (NoSuchKeyException exception) {
            throw notFound(enrollmentId);
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw notFound(enrollmentId);
            }
            throw storageError("No fue posible descargar el resumen desde S3.", exception);
        }
    }

    public void deleteSummary(Long enrollmentId) {
        String key = summaryKey(enrollmentId);
        ensureSummaryExists(key, enrollmentId);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception exception) {
            throw storageError("No fue posible eliminar el resumen desde S3.", exception);
        }
    }

    private StoredFileResponse putSummary(Long enrollmentId, String fileName, String contentType, byte[] content) {
        String key = summaryKey(enrollmentId);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));

            return new StoredFileResponse(
                    enrollmentId,
                    bucketName,
                    key,
                    fileName,
                    contentType,
                    content.length
            );
        } catch (S3Exception exception) {
            throw storageError("No fue posible guardar el resumen en S3.", exception);
        }
    }

    private void ensureSummaryExists(String key, Long enrollmentId) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException exception) {
            throw notFound(enrollmentId);
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw notFound(enrollmentId);
            }
            throw storageError("No fue posible validar el resumen en S3.", exception);
        }
    }

    private String summaryKey(Long enrollmentId) {
        String cleanPrefix = summaryPrefix.replaceAll("^/+|/+$", "");
        String fileName = summaryFileService.fileName(enrollmentId);
        if (cleanPrefix.isBlank()) {
            return enrollmentId + "/" + fileName;
        }
        return cleanPrefix + "/" + enrollmentId + "/" + fileName;
    }

    private String contentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    private boolean isNotFound(S3Exception exception) {
        return exception.statusCode() == 404;
    }

    private ResourceNotFoundException notFound(Long enrollmentId) {
        return new ResourceNotFoundException(
                "No existe un resumen almacenado en S3 para la inscripcion: " + enrollmentId
        );
    }

    private StorageOperationException storageError(String message, S3Exception exception) {
        return new StorageOperationException(message, exception);
    }
}
