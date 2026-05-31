package cl.education.enrollment.service;

import cl.education.enrollment.dto.EnrolledCourseResponse;
import cl.education.enrollment.dto.EnrollmentRequest;
import cl.education.enrollment.dto.EnrollmentSummaryResponse;
import cl.education.enrollment.exception.BusinessRuleException;
import cl.education.enrollment.exception.ResourceNotFoundException;
import cl.education.enrollment.model.Course;
import cl.education.enrollment.model.Enrollment;
import cl.education.enrollment.model.EnrollmentItem;
import cl.education.enrollment.repository.CourseRepository;
import cl.education.enrollment.repository.EnrollmentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public EnrollmentSummaryResponse enroll(EnrollmentRequest request) {
        LinkedHashSet<Long> requestedCourseIds = new LinkedHashSet<>(request.courseIds());
        if (requestedCourseIds.size() != request.courseIds().size()) {
            throw new BusinessRuleException("La inscripción no puede contener cursos repetidos.");
        }

        List<Course> courses = courseRepository.findAllById(requestedCourseIds);
        Map<Long, Course> coursesById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<Long> missingCourseIds = requestedCourseIds.stream()
                .filter(courseId -> !coursesById.containsKey(courseId))
                .toList();
        if (!missingCourseIds.isEmpty()) {
            throw new ResourceNotFoundException("No existen cursos para los ids: " + missingCourseIds);
        }

        Enrollment enrollment = new Enrollment(request.studentName(), request.studentEmail());
        requestedCourseIds.stream()
                .map(coursesById::get)
                .map(course -> new EnrollmentItem(course, course.getName(), course.getCost()))
                .forEach(enrollment::addItem);
        enrollment.recalculateTotal();

        return toSummary(enrollmentRepository.save(enrollment));
    }

    private EnrollmentSummaryResponse toSummary(Enrollment enrollment) {
        List<EnrolledCourseResponse> courses = enrollment.getItems()
                .stream()
                .map(item -> new EnrolledCourseResponse(
                        item.getCourse().getId(),
                        item.getCourseName(),
                        item.getCourseCost()
                ))
                .toList();

        return new EnrollmentSummaryResponse(
                enrollment.getId(),
                enrollment.getStudentName(),
                enrollment.getStudentEmail(),
                courses,
                enrollment.getTotalCost(),
                enrollment.getCreatedAt()
        );
    }
}
