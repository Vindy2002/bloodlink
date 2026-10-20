package lk.bloodlink.repo;

import lk.bloodlink.domain.RequestResponse;
import lk.bloodlink.domain.ResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RequestResponseRepository extends JpaRepository<RequestResponse, Long> {
    List<RequestResponse> findByRequestId(Long requestId);
    List<RequestResponse> findByDonorIdOrderByIdDesc(Long donorId);
    Optional<RequestResponse> findByRequestIdAndDonorId(Long requestId, Long donorId);
    boolean existsByRequestIdAndDonorId(Long requestId, Long donorId);
    long countByRequestIdAndStatus(Long requestId, ResponseStatus status);
}
