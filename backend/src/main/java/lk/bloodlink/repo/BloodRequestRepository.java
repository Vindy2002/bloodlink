package lk.bloodlink.repo;

import lk.bloodlink.domain.BloodRequest;
import lk.bloodlink.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
    long countByStatus(RequestStatus status);
}
