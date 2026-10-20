package lk.bloodlink.repo;

import lk.bloodlink.domain.DonationRecord;
import lk.bloodlink.domain.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationRecordRepository extends JpaRepository<DonationRecord, Long> {
    List<DonationRecord> findByDonorIdOrderByDonationDateDesc(Long donorId);
    List<DonationRecord> findByHospitalIdOrderByDonationDateDesc(Long hospitalId);
    long countByStatus(DonationStatus status);
}
