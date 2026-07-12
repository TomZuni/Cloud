package cl.education.enrollment.repository;

import cl.education.enrollment.model.DispatchGuide;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispatchGuideRepository extends JpaRepository<DispatchGuide, Long> {

    @Query("""
            select guide
            from DispatchGuide guide
            where (:carrierName is null or lower(guide.carrierName) like lower(concat('%', :carrierName, '%')))
              and (:dispatchDate is null or guide.dispatchDate = :dispatchDate)
            order by guide.createdAt desc
            """)
    List<DispatchGuide> search(
            @Param("carrierName") String carrierName,
            @Param("dispatchDate") LocalDate dispatchDate
    );
}
