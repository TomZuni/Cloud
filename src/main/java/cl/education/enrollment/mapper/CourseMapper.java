package cl.education.enrollment.mapper;

import cl.education.enrollment.dto.CourseResponse;
import cl.education.enrollment.model.Course;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getInstructor(),
                course.getDurationHours(),
                course.getCost()
        );
    }
}
