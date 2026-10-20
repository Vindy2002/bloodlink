package lk.bloodlink.web;

import lk.bloodlink.domain.User;
import lk.bloodlink.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal User u) { return notifications.list(u); }

    @PostMapping("/read-all")
    public void readAll(@AuthenticationPrincipal User u) { notifications.markAllSeen(u); }

    @PostMapping("/{id}/read")
    public void read(@AuthenticationPrincipal User u, @PathVariable Long id) { notifications.markSeen(u, id); }
}
