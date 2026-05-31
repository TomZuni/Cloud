package cl.education.enrollment.repository;

import cl.education.enrollment.model.Enrollment;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = {"items", "items.course"})
    Optional<Enrollment> findWithItemsById(Long id);
}
