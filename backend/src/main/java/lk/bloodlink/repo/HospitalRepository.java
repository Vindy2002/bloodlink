package lk.bloodlink.repo;

import lk.bloodlink.domain.Hospital;
import lk.bloodlink.domain.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByUserId(Long userId);
    List<Hospital> findByStatusOrderByCreatedAtDesc(VerificationStatus status);
    long countByStatus(VerificationStatus status);
}
