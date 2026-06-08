package cl.education.enrollment.service;

import cl.education.enrollment.dto.CreateDispatchGuideRequest;
import cl.education.enrollment.dto.DispatchGuideResponse;
import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.dto.StoredDispatchGuideResponse;
import cl.education.enrollment.dto.UpdateDispatchGuideRequest;
import cl.education.enrollment.exception.ForbiddenOperationException;
import cl.education.enrollment.exception.ResourceNotFoundException;
import cl.education.enrollment.exception.StorageOperationException;
import cl.education.enrollment.model.DispatchGuide;
import cl.education.enrollment.repository.DispatchGuideRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class DispatchGuideService {

    private static final String CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DispatchGuideRepository dispatchGuideRepository;
    private final DispatchGuidePdfService pdfService;
    private final S3Client s3Client;
    private final Path efsPath;
    private final String bucketName;
    private final String s3Prefix;

    public DispatchGuideService(
            DispatchGuideRepository dispatchGuideRepository,
            DispatchGuidePdfService pdfService,
            S3Client s3Client,
            @Value("${dispatch-guides.efs-path}") String efsPath,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${dispatch-guides.s3-prefix}") String s3Prefix
    ) {
        this.dispatchGuideRepository = dispatchGuideRepository;
        this.pdfService = pdfService;
        this.s3Client = s3Client;
        this.efsPath = Path.of(efsPath);
        this.bucketName = bucketName;
        this.s3Prefix = s3Prefix;
    }

    @Transactional
    public DispatchGuideResponse create(CreateDispatchGuideRequest request) {
        DispatchGuide guide = new DispatchGuide(
                request.orderNumber(),
                request.carrierName(),
                request.carrierRut(),
                request.recipientName(),
                request.originAddress(),
                request.destinationAddress(),
                request.packageDescription(),
                request.dispatchDate(),
                UUID.randomUUID().toString().replace("-", "")
        );

        DispatchGuide saved = dispatchGuideRepository.save(guide);
        writeLocalGuide(saved);
        return toResponse(dispatchGuideRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public List<DispatchGuideResponse> search(String carrierName, LocalDate dispatchDate) {
        String carrierFilter = carrierName == null || carrierName.isBlank() ? null : carrierName.trim();
        return dispatchGuideRepository.search(carrierFilter, dispatchDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DispatchGuideResponse get(Long guideId) {
        return toResponse(findGuide(guideId));
    }

    @Transactional
    public DispatchGuideResponse update(Long guideId, UpdateDispatchGuideRequest request) {
        DispatchGuide guide = findGuide(guideId);
        String previousS3Key = guide.getS3Key();

        guide.updateDetails(
                request.orderNumber(),
                request.carrierName(),
                request.carrierRut(),
                request.recipientName(),
                request.originAddress(),
                request.destinationAddress(),
                request.packageDescription(),
                request.dispatchDate()
        );
        writeLocalGuide(guide);

        if (previousS3Key != null && !previousS3Key.isBlank()) {
            deleteS3Object(previousS3Key);
            putGuide(guide, s3Key(guide));
        }

        return toResponse(dispatchGuideRepository.save(guide));
    }

    @Transactional(readOnly = true)
    public GeneratedFile downloadLocal(Long guideId) {
        DispatchGuide guide = findGuide(guideId);
        try {
            Path path = localPath(guide);
            if (!Files.exists(path)) {
                throw new ResourceNotFoundException("No existe el archivo temporal EFS para la guia: " + guideId);
            }
            return new GeneratedFile(guide.getFileName(), CONTENT_TYPE, Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new StorageOperationException("No fue posible leer la guia desde EFS.", exception);
        }
    }

    @Transactional
    public StoredDispatchGuideResponse uploadToS3(Long guideId) {
        DispatchGuide guide = findGuide(guideId);
        ensureLocalFile(guide);
        String key = s3Key(guide);
        return putGuide(guide, key);
    }

    @Transactional(readOnly = true)
    public GeneratedFile downloadFromS3(Long guideId, String accessCode) {
        DispatchGuide guide = findGuide(guideId);
        validateAccess(guide, accessCode);
        if (guide.getS3Key() == null || guide.getS3Key().isBlank()) {
            throw new ResourceNotFoundException("La guia no se ha subido a S3: " + guideId);
        }

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(guide.getS3Key())
                    .build());
            return new GeneratedFile(guide.getFileName(), contentType(response.response().contentType()), response.asByteArray());
        } catch (NoSuchKeyException exception) {
            throw new ResourceNotFoundException("No existe la guia en S3: " + guideId);
        } catch (SdkException exception) {
            throw new StorageOperationException("No fue posible descargar la guia desde S3.", exception);
        }
    }

    @Transactional
    public void delete(Long guideId) {
        DispatchGuide guide = findGuide(guideId);
        if (guide.getS3Key() != null && !guide.getS3Key().isBlank()) {
            deleteS3Object(guide.getS3Key());
        }
        deleteLocalFile(guide);
        dispatchGuideRepository.delete(guide);
    }

    private void writeLocalGuide(DispatchGuide guide) {
        try {
            Files.createDirectories(localDirectory(guide));
            String fileName = fileName(guide.getId());
            Path filePath = localDirectory(guide).resolve(fileName);
            Files.write(filePath, pdfService.generate(guide));
            guide.markLocalFile(fileName, filePath.toString());
        } catch (IOException | IllegalStateException exception) {
            throw new StorageOperationException("No fue posible guardar la guia en EFS.", exception);
        }
    }

    private void ensureLocalFile(DispatchGuide guide) {
        Path path = localPath(guide);
        if (!Files.exists(path)) {
            writeLocalGuide(guide);
        }
    }

    private StoredDispatchGuideResponse putGuide(DispatchGuide guide, String key) {
        try {
            byte[] content = Files.readAllBytes(localPath(guide));
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(CONTENT_TYPE)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));

            guide.markUploaded(key);
            dispatchGuideRepository.save(guide);
            return new StoredDispatchGuideResponse(guide.getId(), bucketName, key, guide.getFileName(), CONTENT_TYPE, content.length);
        } catch (IOException exception) {
            throw new StorageOperationException("No fue posible leer la guia temporal desde EFS.", exception);
        } catch (SdkException exception) {
            throw new StorageOperationException("No fue posible guardar la guia en S3.", exception);
        }
    }

    private void deleteS3Object(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw new StorageOperationException("No fue posible eliminar la guia desde S3.", exception);
            }
        } catch (SdkException exception) {
            throw new StorageOperationException("No fue posible eliminar la guia desde S3.", exception);
        }
    }

    private void deleteLocalFile(DispatchGuide guide) {
        try {
            if (guide.getLocalFilePath() != null && !guide.getLocalFilePath().isBlank()) {
                Files.deleteIfExists(Path.of(guide.getLocalFilePath()));
            }
        } catch (IOException exception) {
            throw new StorageOperationException("No fue posible eliminar la guia temporal en EFS.", exception);
        }
    }

    private void validateAccess(DispatchGuide guide, String accessCode) {
        if (accessCode == null || !guide.getAccessCode().equals(accessCode)) {
            throw new ForbiddenOperationException("No tiene permisos para descargar esta guia.");
        }
    }

    private DispatchGuide findGuide(Long guideId) {
        return dispatchGuideRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una guia de despacho con id: " + guideId));
    }

    private Path localDirectory(DispatchGuide guide) {
        return efsPath
                .resolve(DATE_PATH_FORMAT.format(guide.getDispatchDate()))
                .resolve(slug(guide.getCarrierName()));
    }

    private Path localPath(DispatchGuide guide) {
        if (guide.getLocalFilePath() != null && !guide.getLocalFilePath().isBlank()) {
            return Path.of(guide.getLocalFilePath());
        }
        return localDirectory(guide).resolve(fileName(guide.getId()));
    }

    private String s3Key(DispatchGuide guide) {
        String cleanPrefix = s3Prefix.replaceAll("^/+|/+$", "");
        String relativeKey = DATE_PATH_FORMAT.format(guide.getDispatchDate())
                + "/" + slug(guide.getCarrierName())
                + "/" + fileName(guide.getId());
        if (cleanPrefix.isBlank()) {
            return relativeKey;
        }
        return cleanPrefix + "/" + relativeKey;
    }

    private String fileName(Long guideId) {
        return "guia-despacho-" + guideId + ".pdf";
    }

    private String slug(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "transportista" : normalized;
    }

    private String contentType(String contentType) {
        return contentType == null || contentType.isBlank() ? CONTENT_TYPE : contentType;
    }

    private DispatchGuideResponse toResponse(DispatchGuide guide) {
        return new DispatchGuideResponse(
                guide.getId(),
                guide.getOrderNumber(),
                guide.getCarrierName(),
                guide.getCarrierRut(),
                guide.getRecipientName(),
                guide.getOriginAddress(),
                guide.getDestinationAddress(),
                guide.getPackageDescription(),
                guide.getDispatchDate(),
                guide.getAccessCode(),
                guide.getFileName(),
                guide.getLocalFilePath(),
                guide.getS3Key(),
                guide.isUploadedToS3(),
                guide.getCreatedAt(),
                guide.getUpdatedAt()
        );
    }
}
