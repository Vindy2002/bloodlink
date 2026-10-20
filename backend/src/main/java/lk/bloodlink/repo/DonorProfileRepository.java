package lk.bloodlink.repo;

import lk.bloodlink.domain.BloodGroup;
import lk.bloodlink.domain.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {
    Optional<DonorProfile> findByUserId(Long userId);
    Optional<DonorProfile> findByUserEmailIgnoreCase(String email);
    List<DonorProfile> findByBloodGroupInAndAvailableTrue(Collection<BloodGroup> groups);

    /** Cheap bounding-box pre-filter; exact Haversine distance is applied afterwards in Java. */
    @Query("select d from DonorProfile d where d.bloodGroup in :groups and d.available = true " +
           "and d.latitude between :minLat and :maxLat and d.longitude between :minLng and :maxLng")
    List<DonorProfile> findInBox(@Param("groups") Collection<BloodGroup> groups,
                                 @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                 @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    @Query("select d.bloodGroup, count(d) from DonorProfile d where d.available = true and d.visibleInSearch = true group by d.bloodGroup")
    List<Object[]> countByGroup();
}
