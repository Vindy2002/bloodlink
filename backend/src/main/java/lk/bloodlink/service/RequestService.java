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

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final BloodRequestRepository requests;
    private final RequestResponseRepository responses;
    private final DonorProfileRepository donors;
    private final HospitalService hospitalService;
    private final NotificationService notifier;
    private final AppProperties props;

    /** Creates a request and instantly alerts every eligible compatible donor inside the radius. */
    @Transactional
    public RequestDto create(User u, CreateRequest in) {
        Hospital h = hospitalService.verifiedOf(u);
        BloodRequest q = new BloodRequest();
        q.setHospital(h);
        q.setBloodGroup(in.bloodGroup());
        q.setUnitsNeeded(in.unitsNeeded());
        q.setUrgency(in.urgency());
        q.setNote(in.note());
        q.setNeededBy(in.neededBy());
        q.setLatitude(in.latitude() != null ? in.latitude() : h.getLatitude());
        q.setLongitude(in.longitude() != null ? in.longitude() : h.getLongitude());
        q.setRadiusKm(in.radiusKm() != null ? in.radiusKm() : (in.urgency() == Urgency.CRITICAL ? 30 : 15));
        requests.save(q);
        notifyMatching(q);
        return toDto(q);
    }

    @Transactional
    public RequestDto escalate(User u, Long id) {
        BloodRequest q = owned(u, id);
        if (q.getStatus() != RequestStatus.OPEN) throw new ApiException(HttpStatus.CONFLICT, "Request is not open.");
        q.setRadiusKm(Math.min(200, q.getRadiusKm() + 10));
        notifyMatching(q);   // already-notified donors are skipped
        return toDto(q);
    }

    @Transactional
    public RequestDto setStatus(User u, Long id, RequestStatus s) {
        BloodRequest q = owned(u, id);
        q.setStatus(s);
        return toDto(q);
    }

    @Transactional(readOnly = true)
    public List<RequestDto> list(User u) {
        Hospital h = hospitalService.of(u);
        return requests.findByHospitalIdOrderByCreatedAtDesc(h.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ResponseDto> responsesFor(User u, Long id) {
        BloodRequest q = owned(u, id);
        return responses.findByRequestId(q.getId()).stream().map(r -> {
            DonorProfile d = r.getDonor();
            Double km = d.getLatitude() == null ? null
                    : Geo.round1(Geo.km(q.getLatitude(), q.getLongitude(), d.getLatitude(), d.getLongitude()));
            // Accepting a request = consent to share the phone number with this hospital.
            String phone = r.getStatus() == ResponseStatus.ACCEPTED ? d.getUser().getPhone() : null;
            String name = r.getStatus() == ResponseStatus.ACCEPTED ? d.getUser().getFullName() : Geo.maskName(d.getUser().getFullName());
            return new ResponseDto(d.getId(), name, d.getBloodGroup(), km, r.getStatus(), d.isVerifiedDonor(), phone, r.getRespondedAt());
        }).sorted(java.util.Comparator.comparingInt(x -> switch (x.status()) { case ACCEPTED -> 0; case NOTIFIED -> 1; case DECLINED -> 2; }))
                .toList();
    }

    private void notifyMatching(BloodRequest q) {
        List<BloodGroup> groups = q.getBloodGroup().donorsFor();
        double[] b = Geo.box(q.getLatitude(), q.getLongitude(), q.getRadiusKm());
        for (DonorProfile d : donors.findInBox(groups, b[0], b[1], b[2], b[3])) {
            if (!d.isEligible(props.donationGapMonths())) continue;
            double km = Geo.km(q.getLatitude(), q.getLongitude(), d.getLatitude(), d.getLongitude());
            if (km > q.getRadiusKm() || km > d.getAlertRadiusKm()) continue;
            if (responses.existsByRequestIdAndDonorId(q.getId(), d.getId())) continue;
            RequestResponse r = new RequestResponse();
            r.setRequest(q);
            r.setDonor(d);
            responses.save(r);
            String title = q.getUrgency() + ": " + q.getBloodGroup().getLabel() + " blood needed";
            String msg = q.getHospital().getName() + ", " + q.getHospital().getCity() + " needs "
                    + q.getUnitsNeeded() + " unit(s) of " + q.getBloodGroup().getLabel() + " (~"
                    + Math.round(km) + " km from you). Open BloodLink to accept or decline.";
            notifier.alert(d.getUser(), title, msg, q.getId(), d.isReceiveSms(), d.isReceiveEmail());
        }
    }

    private BloodRequest owned(User u, Long id) {
        Hospital h = hospitalService.of(u);
        BloodRequest q = requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!q.getHospital().getId().equals(h.getId())) throw new ApiException(HttpStatus.NOT_FOUND, "Request not found");
        return q;
    }

    private RequestDto toDto(BloodRequest q) {
        return new RequestDto(q.getId(), q.getBloodGroup(), q.getUnitsNeeded(), q.getUrgency(), q.getNote(), q.getRadiusKm(),
                q.getStatus(), q.getCreatedAt(), q.getNeededBy(), q.getHospital().getName(),
                responses.countByRequestIdAndStatus(q.getId(), ResponseStatus.NOTIFIED),
                responses.countByRequestIdAndStatus(q.getId(), ResponseStatus.ACCEPTED),
                responses.countByRequestIdAndStatus(q.getId(), ResponseStatus.DECLINED));
    }
}
