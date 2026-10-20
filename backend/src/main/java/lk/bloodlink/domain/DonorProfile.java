package lk.bloodlink.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "donor_profiles")
@Getter @Setter @NoArgsConstructor
public class DonorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BloodGroup bloodGroup;
    private String gender;
    private LocalDate dateOfBirth;
    private String city;
    private String district;
    private Integer weightKg;
    private Boolean regularDonor = false;   // allows donating up to age 60
    private Double latitude;
    private Double longitude;

    private boolean available = true;
    private LocalDate lastDonationDate;
    /** true after a hospital confirmed at least one donation */
    private boolean verifiedDonor = false;

    // privacy controls
    private boolean showPhone = false;          // phone visible to verified hospitals in search
    private boolean hideExactLocation = true;   // coordinates are blurred more strongly
    private boolean visibleInSearch = true;     // appear in public search at all
    private boolean receiveSms = true;
    private boolean receiveEmail = true;
    private int alertRadiusKm = 30;             // donor's own max travel distance for alerts

    public LocalDate nextEligibleDate(int gapMonths) {
        return lastDonationDate == null ? null : lastDonationDate.plusMonths(gapMonths);
    }

    public boolean isEligible(int gapMonths) {
        if (!available) return false;
        if (dateOfBirth != null) {
            int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
            int max = Boolean.TRUE.equals(regularDonor) ? 60 : 55;
            if (age < 18 || age > max) return false;
        }
        LocalDate next = nextEligibleDate(gapMonths);
        return next == null || !next.isAfter(LocalDate.now());
    }
}
