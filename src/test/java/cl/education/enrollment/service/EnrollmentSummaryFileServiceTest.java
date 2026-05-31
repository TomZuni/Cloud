package cl.education.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.model.Course;
import cl.education.enrollment.model.Enrollment;
import cl.education.enrollment.model.EnrollmentItem;
import cl.education.enrollment.repository.EnrollmentRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EnrollmentSummaryFileServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentSummaryFileService summaryFileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        summaryFileService = new EnrollmentSummaryFileService(enrollmentRepository);
    }

    @Test
    void generatesDownloadableEnrollmentSummary() {
        Enrollment enrollment = enrollment();
        when(enrollmentRepository.findWithItemsById(10L)).thenReturn(Optional.of(enrollment));

        GeneratedFile file = summaryFileService.generate(10L);
        String content = new String(file.content(), StandardCharsets.UTF_8);

        assertThat(file.fileName()).isEqualTo("resumen-inscripcion-10.txt");
        assertThat(file.contentType()).isEqualTo("text/plain; charset=UTF-8");
        assertThat(content).contains("Resumen de inscripcion #10");
        assertThat(content).contains("Camila Perez");
        assertThat(content).contains("Spring Boot");
        assertThat(content).contains("Total a pagar: 150000.00");
    }

    private Enrollment enrollment() {
        Course course = new Course("Spring Boot", "Ana Soto", 32, new BigDecimal("150000"));
        setField(course, "id", 1L);

        Enrollment enrollment = new Enrollment("Camila Perez", "camila@example.com");
        setField(enrollment, "id", 10L);
        setField(enrollment, "createdAt", Instant.parse("2026-05-25T12:00:00Z"));
        enrollment.addItem(new EnrollmentItem(course, course.getName(), course.getCost()));
        enrollment.recalculateTotal();

        return enrollment;
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
