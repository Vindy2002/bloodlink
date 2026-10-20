package lk.bloodlink.service;

import lk.bloodlink.config.AppProperties;
import lk.bloodlink.domain.*;
import lk.bloodlink.repo.*;
import lk.bloodlink.web.Dtos.DonorSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final DonorProfileRepository donors;
    private final HospitalRepository hospitals;
    private final AppProperties props;

    /**
     * Geo search. Only donors that are available, visible, and past the donation gap are returned.
     * Exact coordinates never leave the server (they are blurred); phone numbers are only shown to
     * verified hospitals / admins and only if the donor allowed it.
     */
    @Transactional(readOnly = true)
    public List<DonorSearchResult> search(BloodGroup group, Double lat, Double lng, int radiusKm,
                                          boolean compatible, String area, User viewer) {
        List<BloodGroup> groups = compatible ? group.donorsFor() : List.of(group);
        boolean geo = lat != null && lng != null;
        double[] b = geo ? Geo.box(lat, lng, radiusKm) : null;
        List<DonorProfile> candidates = geo
                ? donors.findInBox(groups, b[0], b[1], b[2], b[3])
                : donors.findByBloodGroupInAndAvailableTrue(groups);
        boolean privileged = isPrivileged(viewer);
        String a = area == null ? "" : area.trim().toLowerCase();
        int gap = props.donationGapMonths();

        return candidates.stream()
                .filter(DonorProfile::isVisibleInSearch)
                .filter(d -> d.isEligible(gap))
                .filter(d -> a.isEmpty() || contains(d.getCity(), a) || contains(d.getDistrict(), a))
                .map(d -> {
                    Double km = geo && d.getLatitude() != null
                            ? Geo.round1(Geo.km(lat, lng, d.getLatitude(), d.getLongitude())) : null;
                    double spread = d.isHideExactLocation() ? 0.012 : 0.004;
                    double bl = d.getLatitude() == null ? 0 : Geo.blur(d.getLatitude(), d.getId(), spread);
                    double bg = d.getLongitude() == null ? 0 : Geo.blur(d.getLongitude(), d.getId() + 7919, spread);
                    String phone = privileged && d.isShowPhone() ? d.getUser().getPhone() : null;
                    return new DonorSearchResult(d.getId(), Geo.maskName(d.getUser().getFullName()), d.getBloodGroup(),
                            d.getBloodGroup() == group, d.getCity(), km, bl, bg, d.isVerifiedDonor(), phone);
                })
                .filter(r -> r.distanceKm() == null || r.distanceKm() <= radiusKm)
                .sorted(Comparator.comparing(DonorSearchResult::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(60)
                .toList();
    }

    private boolean contains(String v, String needle) {
        return v != null && v.toLowerCase().contains(needle);
    }

    private boolean isPrivileged(User u) {
        if (u == null) return false;
        if (u.getRole() == Role.ADMIN) return true;
        return u.getRole() == Role.HOSPITAL && hospitals.findByUserId(u.getId())
                .map(h -> h.getStatus() == VerificationStatus.VERIFIED).orElse(false);
    }
}
