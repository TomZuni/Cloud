package cl.education.enrollment.controller;

import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.dto.StoredFileResponse;
import cl.education.enrollment.service.EnrollmentSummaryFileService;
import cl.education.enrollment.service.EnrollmentSummaryStorageService;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Profile("!dispatch-guides")
@RequestMapping("/api/enrollments")
public class EnrollmentSummaryController {

    private final EnrollmentSummaryFileService summaryFileService;
    private final EnrollmentSummaryStorageService summaryStorageService;

    public EnrollmentSummaryController(
            EnrollmentSummaryFileService summaryFileService,
            EnrollmentSummaryStorageService summaryStorageService
    ) {
        this.summaryFileService = summaryFileService;
        this.summaryStorageService = summaryStorageService;
    }

    @GetMapping("/{enrollmentId}/summary")
    public ResponseEntity<byte[]> downloadGeneratedSummary(@PathVariable Long enrollmentId) {
        return downloadable(summaryFileService.generate(enrollmentId));
    }

    @PostMapping("/{enrollmentId}/summary/s3")
    @ResponseStatus(HttpStatus.CREATED)
    public StoredFileResponse uploadGeneratedSummary(@PathVariable Long enrollmentId) {
        return summaryStorageService.uploadGeneratedSummary(enrollmentId);
    }

    @PutMapping(
            path = "/{enrollmentId}/summary/s3",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public StoredFileResponse replaceSummary(
            @PathVariable Long enrollmentId,
            @RequestParam("file") MultipartFile file
    ) {
        return summaryStorageService.replaceSummary(enrollmentId, file);
    }

    @GetMapping("/{enrollmentId}/summary/s3")
    public ResponseEntity<byte[]> downloadStoredSummary(@PathVariable Long enrollmentId) {
        return downloadable(summaryStorageService.downloadSummary(enrollmentId));
    }

    @DeleteMapping("/{enrollmentId}/summary/s3")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoredSummary(@PathVariable Long enrollmentId) {
        summaryStorageService.deleteSummary(enrollmentId);
    }

    private ResponseEntity<byte[]> downloadable(GeneratedFile file) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
