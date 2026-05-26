package cl.education.enrollment.course;

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
