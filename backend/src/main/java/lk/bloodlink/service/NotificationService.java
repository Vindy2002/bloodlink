package lk.bloodlink.service;

import lk.bloodlink.domain.Notification;
import lk.bloodlink.domain.User;
import lk.bloodlink.repo.NotificationRepository;
import lk.bloodlink.web.ApiException;
import lk.bloodlink.web.Dtos.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final AsyncSender sender;

    @Transactional
    public void inApp(User u, String title, String message, Long requestId) {
        Notification n = new Notification();
        n.setUser(u);
        n.setTitle(title);
        n.setMessage(message);
        n.setRequestId(requestId);
        repo.save(n);
    }

    /** In-app + optional SMS + optional email. */
    @Transactional
    public void alert(User u, String title, String message, Long requestId, boolean sms, boolean email) {
        inApp(u, title, message, requestId);
        if (sms && u.getPhone() != null && !u.getPhone().isBlank()) sender.sms(u.getPhone(), "BloodLink: " + title + " - " + message);
        if (email) sender.email(u.getEmail(), "BloodLink: " + title, message);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(User u) {
        List<NotificationDto> items = repo.findTop30ByUserIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getRequestId(), n.isSeen(), n.getCreatedAt()))
                .toList();
        return Map.of("unread", repo.countByUserIdAndSeenFalse(u.getId()), "items", items);
    }

    @Transactional
    public void markAllSeen(User u) {
        repo.findTop30ByUserIdOrderByCreatedAtDesc(u.getId()).forEach(n -> n.setSeen(true));
    }

    @Transactional
    public void markSeen(User u, Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        if (!n.getUser().getId().equals(u.getId())) throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
        n.setSeen(true);
    }
}
