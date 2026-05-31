package cl.education.enrollment.repository;

import cl.education.enrollment.model.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByAvailableTrueOrderByNameAsc();
}
