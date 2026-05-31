package cl.education.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.education.enrollment.dto.EnrolledCourseResponse;
import cl.education.enrollment.dto.EnrollmentRequest;
import cl.education.enrollment.dto.EnrollmentSummaryResponse;
import cl.education.enrollment.exception.BusinessRuleException;
import cl.education.enrollment.exception.ResourceNotFoundException;
import cl.education.enrollment.model.Course;
import cl.education.enrollment.model.Enrollment;
import cl.education.enrollment.repository.CourseRepository;
import cl.education.enrollment.repository.EnrollmentRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EnrollmentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        enrollmentService = new EnrollmentService(courseRepository, enrollmentRepository);
    }

    @Test
    void createsEnrollmentSummaryWithSelectedCoursesAndTotal() throws Exception {
        Course spring = course(1L, "Spring Boot", "Ana Soto", 32, "150000");
        Course cloud = course(2L, "Cloud Computing", "Luis Rojas", 24, "120000");
        when(courseRepository.findAllById(any())).thenReturn(List.of(spring, cloud));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            setField(enrollment, "id", 10L);
            setField(enrollment, "createdAt", Instant.parse("2026-05-25T12:00:00Z"));
            return enrollment;
        });

        EnrollmentSummaryResponse response = enrollmentService.enroll(new EnrollmentRequest(
                "Camila Perez",
                "camila@example.com",
                List.of(1L, 2L)
        ));

        assertThat(response.enrollmentId()).isEqualTo(10L);
        assertThat(response.courses()).extracting(EnrolledCourseResponse::name)
                .containsExactly("Spring Boot", "Cloud Computing");
        assertThat(response.totalCost()).isEqualByComparingTo("270000");
    }

    @Test
    void rejectsDuplicatedCourseIds() {
        EnrollmentRequest request = new EnrollmentRequest(
                "Camila Perez",
                "camila@example.com",
                List.of(1L, 1L)
        );

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cursos repetidos");
    }

    @Test
    void failsWhenAnyRequestedCourseDoesNotExist() {
        Course spring = course(1L, "Spring Boot", "Ana Soto", 32, "150000");
        when(courseRepository.findAllById(any())).thenReturn(List.of(spring));

        EnrollmentRequest request = new EnrollmentRequest(
                "Camila Perez",
                "camila@example.com",
                List.of(1L, 9L)
        );

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("[9]");
    }

    private Course course(Long id, String name, String instructor, int durationHours, String cost) {
        Course course = new Course(name, instructor, durationHours, new BigDecimal(cost));
        setField(course, "id", id);
        return course;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
