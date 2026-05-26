package cl.education.enrollment.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByAvailableTrueOrderByNameAsc();
}
