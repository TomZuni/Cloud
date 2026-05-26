package cl.education.enrollment.course;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listAvailableCourses() {
        return courseRepository.findByAvailableTrueOrderByNameAsc()
                .stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        Course course = new Course(
                request.name(),
                request.instructor(),
                request.durationHours(),
                request.cost()
        );

        return CourseMapper.toResponse(courseRepository.save(course));
    }
}
