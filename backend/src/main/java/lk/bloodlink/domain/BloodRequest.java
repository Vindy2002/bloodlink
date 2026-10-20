package lk.bloodlink.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests")
@Getter @Setter @NoArgsConstructor
public class BloodRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Hospital hospital;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BloodGroup bloodGroup;
    private int unitsNeeded = 1;
    @Enumerated(EnumType.STRING)
    private Urgency urgency = Urgency.URGENT;
    @Column(length = 500)
    private String note;               // never store patient names here
    private double latitude;
    private double longitude;
    private int radiusKm = 15;
    private LocalDateTime neededBy;
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.OPEN;
    private Instant createdAt = Instant.now();
}
