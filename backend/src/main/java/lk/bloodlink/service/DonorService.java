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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonorService {
    private final DonorProfileRepository donors;
    private final UserRepository users;
    private final RequestResponseRepository responses;
    private final DonationRecordRepository donations;
    private final NotificationService notifier;
    private final AppProperties props;

    public DonorProfile profileOf(User u) {
        return donors.findByUserId(u.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donor profile not found"));
    }

    @Transactional(readOnly = true)
    public DonorProfileDto get(User u) { return toDto(profileOf(u)); }

    @Transactional
    public DonorProfileDto update(User u, DonorProfileDto in) {
        DonorProfile d = profileOf(u);
        User user = users.findById(u.getId()).orElseThrow();
        if (in.fullName() != null && !in.fullName().isBlank()) user.setFullName(in.fullName());
        if (in.phone() != null && !in.phone().isBlank()) user.setPhone(in.phone());
        if (in.bloodGroup() != null && !d.isVerifiedDonor()) d.setBloodGroup(in.bloodGroup()); // locked once a hospital confirmed it
        d.setGender(in.gender());
        if (in.city() != null) d.setCity(in.city());
        d.setDistrict(in.district());
        if (in.latitude() != null && in.longitude() != null) {
            d.setLatitude(in.latitude());
            d.setLongitude(in.longitude());
        }
        d.setShowPhone(in.showPhone());
        d.setHideExactLocation(in.hideExactLocation());
        d.setVisibleInSearch(in.visibleInSearch());
        d.setReceiveSms(in.receiveSms());
        d.setReceiveEmail(in.receiveEmail());
        d.setAlertRadiusKm(Math.max(1, Math.min(200, in.alertRadiusKm() == 0 ? 30 : in.alertRadiusKm())));
        return toDto(d);
    }

    @Transactional
    public DonorProfileDto setAvailability(User u, boolean available, boolean confirmFit) {
        DonorProfile d = profileOf(u);
        if (available) {
            if (!confirmFit)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Please confirm that you are currently fit to donate.");
            if (d.getDateOfBirth() != null) {
                List<String> p = Eligibility.ageProblems(d.getDateOfBirth(), Boolean.TRUE.equals(d.getRegularDonor()));
                if (!p.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, String.join(" ", p));
            }
        }
        d.setAvailable(available);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<AlertDto> alerts(User u) {
        DonorProfile d = profileOf(u);
        return responses.findByDonorIdOrderByIdDesc(d.getId()).stream().map(r -> {
            BloodRequest q = r.getRequest();
            Double km = d.getLatitude() == null ? null
                    : Geo.round1(Geo.km(d.getLatitude(), d.getLongitude(), q.getLatitude(), q.getLongitude()));
            return new AlertDto(q.getId(), q.getHospital().getName(), q.getHospital().getCity(), q.getBloodGroup(),
                    q.getUnitsNeeded(), q.getUrgency(), q.getNote(), r.getNotifiedAt(), q.getStatus(), r.getStatus(), km);
        }).toList();
    }

    @Transactional
    public void respond(User u, Long requestId, boolean accept) {
        DonorProfile d = profileOf(u);
        RequestResponse r = responses.findByRequestIdAndDonorId(requestId, d.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        BloodRequest q = r.getRequest();
        if (q.getStatus() != RequestStatus.OPEN)
            throw new ApiException(HttpStatus.CONFLICT, "This request is no longer open.");
        if (accept && !d.isEligible(props.donationGapMonths()))
            throw new ApiException(HttpStatus.CONFLICT, "You are not eligible to donate right now.");
        r.setStatus(accept ? ResponseStatus.ACCEPTED : ResponseStatus.DECLINED);
        r.setRespondedAt(Instant.now());
        if (accept) {
            notifier.alert(q.getHospital().getUser(), "Donor accepted",
                    d.getBloodGroup().getLabel() + " donor " + Geo.maskName(u.getFullName())
                            + " accepted your request. Open BloodLink to see contact details.",
                    q.getId(), true, true);
        }
    }

    @Transactional(readOnly = true)
    public List<DonationDto> donations(User u) {
        return donations.findByDonorIdOrderByDonationDateDesc(profileOf(u).getId()).stream()
                .map(r -> new DonationDto(r.getId(), r.getDonationDate(),
                        r.getHospital() != null ? r.getHospital().getName() : r.getPlace(),
                        r.getUnits(), r.getStatus(), r.getNote(), null)).toList();
    }

    @Transactional
    public DonationDto selfReport(User u, SelfReport in) {
        if (in.date().isAfter(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Donation date cannot be in the future.");
        DonorProfile d = profileOf(u);
        DonationRecord rec = new DonationRecord();
        rec.setDonor(d);
        rec.setDonationDate(in.date());
        rec.setPlace(in.place());
        rec.setNote(in.note());
        rec.setStatus(DonationStatus.SELF_REPORTED);
        donations.save(rec);
        if (d.getLastDonationDate() == null || in.date().isAfter(d.getLastDonationDate())) d.setLastDonationDate(in.date());
        return new DonationDto(rec.getId(), rec.getDonationDate(), rec.getPlace(), 1, rec.getStatus(), rec.getNote(), null);
    }

    public DonorProfileDto toDto(DonorProfile d) {
        int gap = props.donationGapMonths();
        User u = d.getUser();
        return new DonorProfileDto(d.getId(), u.getFullName(), u.getEmail(), u.getPhone(), d.getBloodGroup(),
                d.getGender(), d.getDateOfBirth(), d.getCity(), d.getDistrict(), d.getLatitude(), d.getLongitude(),
                d.isAvailable(), d.isEligible(gap), d.getLastDonationDate(), d.nextEligibleDate(gap),
                d.isVerifiedDonor(), d.isShowPhone(), d.isHideExactLocation(), d.isVisibleInSearch(),
                d.isReceiveSms(), d.isReceiveEmail(), d.getAlertRadiusKm());
    }
}
