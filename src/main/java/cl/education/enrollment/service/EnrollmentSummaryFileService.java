package cl.education.enrollment.service;

import cl.education.enrollment.dto.GeneratedFile;
import cl.education.enrollment.exception.ResourceNotFoundException;
import cl.education.enrollment.model.Enrollment;
import cl.education.enrollment.model.EnrollmentItem;
import cl.education.enrollment.repository.EnrollmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentSummaryFileService {

    private static final String CONTENT_TYPE = "text/plain; charset=UTF-8";

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentSummaryFileService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public GeneratedFile generate(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findWithItemsById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una inscripcion con id: " + enrollmentId
                ));

        byte[] content = buildSummary(enrollment).getBytes(StandardCharsets.UTF_8);
        return new GeneratedFile(fileName(enrollmentId), CONTENT_TYPE, content);
    }

    public String fileName(Long enrollmentId) {
        return "resumen-inscripcion-" + enrollmentId + ".txt";
    }

    private String buildSummary(Enrollment enrollment) {
        StringBuilder builder = new StringBuilder();
        builder.append("Resumen de inscripcion #").append(enrollment.getId()).append(System.lineSeparator());
        builder.append("Estudiante: ").append(enrollment.getStudentName()).append(System.lineSeparator());
        builder.append("Email: ").append(enrollment.getStudentEmail()).append(System.lineSeparator());
        builder.append("Fecha de creacion: ").append(enrollment.getCreatedAt()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Cursos seleccionados:").append(System.lineSeparator());

        for (EnrollmentItem item : enrollment.getItems()) {
            builder.append("- ")
                    .append(item.getCourseName())
                    .append(" | Costo: ")
                    .append(formatAmount(item.getCourseCost()))
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("Total a pagar: ").append(formatAmount(enrollment.getTotalCost()));
        builder.append(System.lineSeparator());

        return builder.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
