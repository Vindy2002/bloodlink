package lk.bloodlink.web;

import jakarta.validation.Valid;
import lk.bloodlink.domain.User;
import lk.bloodlink.service.HospitalService;
import lk.bloodlink.service.RequestService;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospital")
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalService hospitals;
    private final RequestService requests;

    @GetMapping("/me")
    public HospitalDto me(@AuthenticationPrincipal User u) { return hospitals.me(u); }

    @PostMapping("/requests")
    public RequestDto create(@AuthenticationPrincipal User u, @Valid @RequestBody CreateRequest in) { return requests.create(u, in); }

    @GetMapping("/requests")
    public List<RequestDto> list(@AuthenticationPrincipal User u) { return requests.list(u); }

    @GetMapping("/requests/{id}/responses")
    public List<ResponseDto> responses(@AuthenticationPrincipal User u, @PathVariable Long id) { return requests.responsesFor(u, id); }

    @PostMapping("/requests/{id}/escalate")
    public RequestDto escalate(@AuthenticationPrincipal User u, @PathVariable Long id) { return requests.escalate(u, id); }

    @PatchMapping("/requests/{id}/status")
    public RequestDto status(@AuthenticationPrincipal User u, @PathVariable Long id, @Valid @RequestBody StatusChange in) {
        return requests.setStatus(u, id, in.status());
    }

    @PostMapping("/donations")
    public DonationDto record(@AuthenticationPrincipal User u, @Valid @RequestBody RecordDonation in) { return hospitals.recordDonation(u, in); }

    @GetMapping("/donations")
    public List<DonationDto> donations(@AuthenticationPrincipal User u) { return hospitals.donationsOf(u); }
}
