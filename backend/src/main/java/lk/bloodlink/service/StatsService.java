package lk.bloodlink.service;

import lk.bloodlink.domain.*;
import lk.bloodlink.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final DonorProfileRepository donors;
    private final HospitalRepository hospitals;
    private final BloodRequestRepository requests;
    private final DonationRecordRepository donations;

    @Transactional(readOnly = true)
    public Map<String, Object> publicStats() {
        Map<String, Long> byGroup = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) byGroup.put(g.name(), 0L);
        for (Object[] row : donors.countByGroup()) byGroup.put(((BloodGroup) row[0]).name(), (Long) row[1]);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("donorsByGroup", byGroup);
        m.put("availableDonors", byGroup.values().stream().mapToLong(Long::longValue).sum());
        m.put("verifiedHospitals", hospitals.countByStatus(VerificationStatus.VERIFIED));
        m.put("openRequests", requests.countByStatus(RequestStatus.OPEN));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminStats() {
        Map<String, Object> m = publicStats();
        m.put("totalDonors", donors.count());
        m.put("pendingHospitals", hospitals.countByStatus(VerificationStatus.PENDING));
        m.put("confirmedDonations", donations.countByStatus(DonationStatus.CONFIRMED));
        m.put("totalRequests", requests.count());
        return m;
    }
}
