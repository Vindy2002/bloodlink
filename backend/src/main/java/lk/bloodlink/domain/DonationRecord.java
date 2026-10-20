package lk.bloodlink.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "donation_records")
@Getter @Setter @NoArgsConstructor
public class DonationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private DonorProfile donor;
    @ManyToOne
    private Hospital hospital;          // null for self-reported donations
    @ManyToOne
    private BloodRequest request;
    private String place;
    private LocalDate donationDate;
    private int units = 1;
    @Enumerated(EnumType.STRING)
    private DonationStatus status = DonationStatus.CONFIRMED;
    private String note;
}
