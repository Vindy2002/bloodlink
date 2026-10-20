package lk.bloodlink.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "request_responses", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "donor_id"}))
@Getter @Setter @NoArgsConstructor
public class RequestResponse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "request_id")
    private BloodRequest request;
    @ManyToOne(optional = false) @JoinColumn(name = "donor_id")
    private DonorProfile donor;
    @Enumerated(EnumType.STRING)
    private ResponseStatus status = ResponseStatus.NOTIFIED;
    private Instant notifiedAt = Instant.now();
    private Instant respondedAt;
}
