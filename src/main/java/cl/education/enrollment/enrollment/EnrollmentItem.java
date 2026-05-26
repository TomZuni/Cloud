package cl.education.enrollment.enrollment;

import cl.education.enrollment.course.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "enrollment_items")
public class EnrollmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_item_seq")
    @SequenceGenerator(name = "enrollment_item_seq", sequenceName = "enrollment_item_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 120)
    private String courseName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal courseCost;

    protected EnrollmentItem() {
    }

    public EnrollmentItem(Course course, String courseName, BigDecimal courseCost) {
        this.course = course;
        this.courseName = courseName;
        this.courseCost = courseCost;
    }

    void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public Course getCourse() {
        return course;
    }

    public String getCourseName() {
        return courseName;
    }

    public BigDecimal getCourseCost() {
        return courseCost;
    }
}
