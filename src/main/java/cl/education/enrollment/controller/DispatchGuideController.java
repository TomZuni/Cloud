package cl.education.enrollment.controller;

import cl.education.enrollment.dto.CreateDispatchGuideRequest;
import cl.education.enrollment.dto.DispatchGuideResponse;
import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.dto.StoredDispatchGuideResponse;
import cl.education.enrollment.dto.UpdateDispatchGuideRequest;
import cl.education.enrollment.service.DispatchGuideService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dispatch-guides")
@RequestMapping("/api/dispatch-guides")
public class DispatchGuideController {

    private final DispatchGuideService dispatchGuideService;

    public DispatchGuideController(DispatchGuideService dispatchGuideService) {
        this.dispatchGuideService = dispatchGuideService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DispatchGuideResponse create(@Valid @RequestBody CreateDispatchGuideRequest request) {
        return dispatchGuideService.create(request);
    }

    @GetMapping
    public List<DispatchGuideResponse> search(
            @RequestParam(required = false) String carrierName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dispatchDate
    ) {
        return dispatchGuideService.search(carrierName, dispatchDate);
    }

    @GetMapping("/{guideId}")
    public DispatchGuideResponse get(@PathVariable Long guideId) {
        return dispatchGuideService.get(guideId);
    }

    @PutMapping("/{guideId}")
    public DispatchGuideResponse update(
            @PathVariable Long guideId,
            @Valid @RequestBody UpdateDispatchGuideRequest request
    ) {
        return dispatchGuideService.update(guideId, request);
    }

    @GetMapping("/{guideId}/efs")
    public ResponseEntity<byte[]> downloadLocal(@PathVariable Long guideId) {
        return downloadable(dispatchGuideService.downloadLocal(guideId));
    }

    @PostMapping("/{guideId}/s3")
    @ResponseStatus(HttpStatus.CREATED)
    public StoredDispatchGuideResponse uploadToS3(@PathVariable Long guideId) {
        return dispatchGuideService.uploadToS3(guideId);
    }

    @GetMapping("/{guideId}/s3")
    public ResponseEntity<byte[]> downloadFromS3(
            @PathVariable Long guideId,
            @RequestParam String accessCode
    ) {
        return downloadable(dispatchGuideService.downloadFromS3(guideId, accessCode));
    }

    @DeleteMapping("/{guideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long guideId) {
        dispatchGuideService.delete(guideId);
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
