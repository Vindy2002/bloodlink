package lk.bloodlink.web;

import jakarta.validation.Valid;
import lk.bloodlink.domain.VerificationStatus;
import lk.bloodlink.service.HospitalService;
import lk.bloodlink.service.StatsService;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final HospitalService hospitals;
    private final StatsService stats;

    @GetMapping("/hospitals")
    public List<HospitalDto> hospitals(@RequestParam(defaultValue = "PENDING") VerificationStatus status) { return hospitals.byStatus(status); }

    @PostMapping("/hospitals/{id}/verify")
    public HospitalDto verify(@PathVariable Long id) { return hospitals.verify(id); }

    @PostMapping("/hospitals/{id}/reject")
    public HospitalDto reject(@PathVariable Long id, @Valid @RequestBody Reject in) { return hospitals.reject(id, in.reason()); }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return stats.adminStats(); }
}
