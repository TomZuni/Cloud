package cl.education.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_seq")
    @SequenceGenerator(name = "course_seq", sequenceName = "course_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String instructor;

    @Column(nullable = false)
    private Integer durationHours;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false)
    private boolean available = true;

    protected Course() {
    }

    public Course(String name, String instructor, Integer durationHours, BigDecimal cost) {
        this.name = name;
        this.instructor = instructor;
        this.durationHours = durationHours;
        this.cost = cost;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInstructor() {
        return instructor;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public boolean isAvailable() {
        return available;
    }
}
