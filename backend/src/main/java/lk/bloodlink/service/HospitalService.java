package lk.bloodlink.service;

import lk.bloodlink.config.AppProperties;
import lk.bloodlink.domain.*;
import lk.bloodlink.repo.*;
import lk.bloodlink.web.ApiException;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitals;
    private final DonorProfileRepository donors;
    private final BloodRequestRepository requests;
    private final DonationRecordRepository donations;
    private final NotificationService notifier;
    private final AppProperties props;

    public Hospital of(User u) {
        return hospitals.findByUserId(u.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hospital profile not found"));
    }

    public Hospital verifiedOf(User u) {
        Hospital h = of(u);
        if (h.getStatus() != VerificationStatus.VERIFIED)
            throw new ApiException(HttpStatus.FORBIDDEN, "Your hospital is not verified yet. An admin must approve it first.");
        return h;
    }

    @Transactional(readOnly = true)
    public HospitalDto me(User u) { return toDto(of(u)); }

    /** Hospital confirms a donation: updates donor's history, last-donation date and verified flag. */
    @Transactional
    public DonationDto recordDonation(User u, RecordDonation in) {
        Hospital h = verifiedOf(u);
        DonorProfile d = in.donorId() != null
                ? donors.findById(in.donorId()).orElse(null)
                : (in.donorEmail() == null ? null : donors.findByUserEmailIgnoreCase(in.donorEmail().trim()).orElse(null));
        if (d == null) throw new ApiException(HttpStatus.NOT_FOUND, "Donor not found.");
        LocalDate date = in.date() == null ? LocalDate.now() : in.date();
        if (date.isAfter(LocalDate.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Date cannot be in the future.");

        DonationRecord rec = new DonationRecord();
        rec.setDonor(d);
        rec.setHospital(h);
        rec.setDonationDate(date);
        rec.setUnits(in.units() == null ? 1 : in.units());
        rec.setNote(in.note());
        rec.setStatus(DonationStatus.CONFIRMED);
        if (in.requestId() != null) {
            BloodRequest q = requests.findById(in.requestId()).orElse(null);
            if (q == null || !q.getHospital().getId().equals(h.getId()))
                throw new ApiException(HttpStatus.NOT_FOUND, "Request not found.");
            rec.setRequest(q);
        }
        donations.save(rec);
        if (d.getLastDonationDate() == null || date.isAfter(d.getLastDonationDate())) d.setLastDonationDate(date);
        d.setVerifiedDonor(true);
        notifier.alert(d.getUser(), "Thank you for donating",
                h.getName() + " recorded your donation. You can donate again after " + d.nextEligibleDate(props.donationGapMonths()) + ".",
                null, false, true);
        return new DonationDto(rec.getId(), date, h.getName(), rec.getUnits(), rec.getStatus(), rec.getNote(),
                Geo.maskName(d.getUser().getFullName()));
    }

    @Transactional(readOnly = true)
    public List<DonationDto> donationsOf(User u) {
        return donations.findByHospitalIdOrderByDonationDateDesc(of(u).getId()).stream()
                .map(r -> new DonationDto(r.getId(), r.getDonationDate(), r.getHospital().getName(), r.getUnits(),
                        r.getStatus(), r.getNote(), Geo.maskName(r.getDonor().getUser().getFullName()))).toList();
    }

    // ---------- admin verification workflow ----------
    @Transactional(readOnly = true)
    public List<HospitalDto> byStatus(VerificationStatus s) {
        return hospitals.findByStatusOrderByCreatedAtDesc(s).stream().map(this::toDto).toList();
    }

    @Transactional
    public HospitalDto verify(Long id) {
        Hospital h = hospitals.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hospital not found"));
        h.setStatus(VerificationStatus.VERIFIED);
        h.setRejectionReason(null);
        notifier.alert(h.getUser(), "Hospital verified", "Your hospital is verified. You can now send blood requests.", null, false, true);
        return toDto(h);
    }

    @Transactional
    public HospitalDto reject(Long id, String reason) {
        Hospital h = hospitals.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hospital not found"));
        h.setStatus(VerificationStatus.REJECTED);
        h.setRejectionReason(reason);
        notifier.alert(h.getUser(), "Hospital verification declined", "Reason: " + reason, null, false, true);
        return toDto(h);
    }

    private HospitalDto toDto(Hospital h) {
        User u = h.getUser();
        return new HospitalDto(h.getId(), h.getName(), h.getRegistrationNo(), h.getAddress(), h.getCity(), h.getStatus(),
                h.getRejectionReason(), u.getFullName(), u.getEmail(), u.getPhone(), h.getCreatedAt());
    }
}
