package cl.education.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dispatch_guides")
public class DispatchGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dispatch_guide_seq")
    @SequenceGenerator(name = "dispatch_guide_seq", sequenceName = "dispatch_guide_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 80)
    private String orderNumber;

    @Column(nullable = false, length = 120)
    private String carrierName;

    @Column(nullable = false, length = 30)
    private String carrierRut;

    @Column(nullable = false, length = 120)
    private String recipientName;

    @Column(nullable = false, length = 180)
    private String originAddress;

    @Column(nullable = false, length = 180)
    private String destinationAddress;

    @Column(nullable = false, length = 500)
    private String packageDescription;

    @Column(nullable = false)
    private LocalDate dispatchDate;

    @Column(nullable = false, length = 64)
    private String accessCode;

    @Column(length = 160)
    private String fileName;

    @Column(length = 500)
    private String localFilePath;

    @Column(length = 500)
    private String s3Key;

    @Column(nullable = false)
    private boolean uploadedToS3;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected DispatchGuide() {
    }

    public DispatchGuide(
            String orderNumber,
            String carrierName,
            String carrierRut,
            String recipientName,
            String originAddress,
            String destinationAddress,
            String packageDescription,
            LocalDate dispatchDate,
            String accessCode
    ) {
        this.orderNumber = orderNumber;
        this.carrierName = carrierName;
        this.carrierRut = carrierRut;
        this.recipientName = recipientName;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.packageDescription = packageDescription;
        this.dispatchDate = dispatchDate;
        this.accessCode = accessCode;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(
            String orderNumber,
            String carrierName,
            String carrierRut,
            String recipientName,
            String originAddress,
            String destinationAddress,
            String packageDescription,
            LocalDate dispatchDate
    ) {
        this.orderNumber = orderNumber;
        this.carrierName = carrierName;
        this.carrierRut = carrierRut;
        this.recipientName = recipientName;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.packageDescription = packageDescription;
        this.dispatchDate = dispatchDate;
    }

    public void markLocalFile(String fileName, String localFilePath) {
        this.fileName = fileName;
        this.localFilePath = localFilePath;
    }

    public void markUploaded(String s3Key) {
        this.s3Key = s3Key;
        uploadedToS3 = true;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getCarrierRut() {
        return carrierRut;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getOriginAddress() {
        return originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public String getPackageDescription() {
        return packageDescription;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public String getS3Key() {
        return s3Key;
    }

    public boolean isUploadedToS3() {
        return uploadedToS3;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
