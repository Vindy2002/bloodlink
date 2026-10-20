package lk.bloodlink.web;

import jakarta.validation.Valid;
import lk.bloodlink.domain.User;
import lk.bloodlink.service.DonorService;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donor")
@RequiredArgsConstructor
public class DonorController {
    private final DonorService donors;

    @GetMapping("/profile")
    public DonorProfileDto profile(@AuthenticationPrincipal User u) { return donors.get(u); }

    @PutMapping("/profile")
    public DonorProfileDto update(@AuthenticationPrincipal User u, @RequestBody DonorProfileDto in) { return donors.update(u, in); }

    @PatchMapping("/availability")
    public DonorProfileDto availability(@AuthenticationPrincipal User u, @RequestBody Map<String, Boolean> body) {
        return donors.setAvailability(u, Boolean.TRUE.equals(body.get("available")), Boolean.TRUE.equals(body.get("confirmFit")));
    }

    @GetMapping("/alerts")
    public List<AlertDto> alerts(@AuthenticationPrincipal User u) { return donors.alerts(u); }

    @PostMapping("/alerts/{requestId}/accept")
    public void accept(@AuthenticationPrincipal User u, @PathVariable Long requestId) { donors.respond(u, requestId, true); }

    @PostMapping("/alerts/{requestId}/decline")
    public void decline(@AuthenticationPrincipal User u, @PathVariable Long requestId) { donors.respond(u, requestId, false); }

    @GetMapping("/donations")
    public List<DonationDto> donations(@AuthenticationPrincipal User u) { return donors.donations(u); }

    @PostMapping("/donations")
    public DonationDto selfReport(@AuthenticationPrincipal User u, @Valid @RequestBody SelfReport in) { return donors.selfReport(u, in); }
}
