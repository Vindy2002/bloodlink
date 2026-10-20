package lk.bloodlink.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "hospitals")
@Getter @Setter @NoArgsConstructor
public class Hospital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private User user;
    @Column(nullable = false)
    private String name;
    private String registrationNo;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    @Enumerated(EnumType.STRING)
    private VerificationStatus status = VerificationStatus.PENDING;
    private String rejectionReason;
    private Instant createdAt = Instant.now();
}
