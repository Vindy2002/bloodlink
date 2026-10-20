package lk.bloodlink.web;

import lk.bloodlink.domain.BloodGroup;
import lk.bloodlink.domain.User;
import lk.bloodlink.service.SearchService;
import lk.bloodlink.service.StatsService;
import lk.bloodlink.web.Dtos.DonorSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SearchController {
    private final SearchService search;
    private final StatsService stats;

    @GetMapping("/api/search/donors")
    public List<DonorSearchResult> donors(@RequestParam BloodGroup bloodGroup,
                                          @RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng,
                                          @RequestParam(defaultValue = "25") int radiusKm,
                                          @RequestParam(defaultValue = "true") boolean compatible,
                                          @RequestParam(required = false) String area,
                                          @AuthenticationPrincipal User viewer) {
        int r = Math.max(1, Math.min(200, radiusKm));
        return search.search(bloodGroup, lat, lng, r, compatible, area, viewer);
    }

    @GetMapping("/api/public/stats")
    public Map<String, Object> publicStats() { return stats.publicStats(); }
}
