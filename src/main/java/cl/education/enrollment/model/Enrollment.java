package cl.education.enrollment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_seq")
    @SequenceGenerator(name = "enrollment_seq", sequenceName = "enrollment_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 120)
    private String studentName;

    @Column(nullable = false, length = 180)
    private String studentEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnrollmentItem> items = new ArrayList<>();

    protected Enrollment() {
    }

    public Enrollment(String studentName, String studentEmail) {
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addItem(EnrollmentItem item) {
        item.setEnrollment(this);
        items.add(item);
    }

    public void recalculateTotal() {
        totalCost = items.stream()
                .map(EnrollmentItem::getCourseCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<EnrollmentItem> getItems() {
        return List.copyOf(items);
    }
}
