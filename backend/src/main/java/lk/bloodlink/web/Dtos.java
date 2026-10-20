package lk.bloodlink.web;

import jakarta.validation.constraints.*;
import lk.bloodlink.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record DonorRegistration(@Email @NotBlank String email, @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
                                    @NotBlank String fullName, @NotBlank String phone, @NotNull BloodGroup bloodGroup,
                                    String gender, @NotNull LocalDate dateOfBirth, @NotNull @Positive Integer weightKg,
                                    Boolean regularDonor, java.util.List<String> conditions,
                                    @NotBlank String city, String district,
                                    @NotNull Double latitude, @NotNull Double longitude) {}

    public record HospitalRegistration(@Email @NotBlank String email, @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
                                       @NotBlank String contactName, @NotBlank String phone, @NotBlank String hospitalName,
                                       @NotBlank String registrationNo, String address, @NotBlank String city,
                                       @NotNull Double latitude, @NotNull Double longitude) {}

    public record UserInfo(Long id, String email, String fullName, Role role, VerificationStatus hospitalStatus) {}
    public record AuthResponse(String token, UserInfo user) {}

    // ---- donor ----
    public record DonorProfileDto(Long id, String fullName, String email, String phone, BloodGroup bloodGroup,
                                  String gender, LocalDate dateOfBirth, String city, String district,
                                  Double latitude, Double longitude, boolean available, boolean eligible,
                                  LocalDate lastDonationDate, LocalDate nextEligibleDate, boolean verifiedDonor,
                                  boolean showPhone, boolean hideExactLocation, boolean visibleInSearch,
                                  boolean receiveSms, boolean receiveEmail, int alertRadiusKm) {}

    public record AlertDto(Long requestId, String hospitalName, String hospitalCity, BloodGroup bloodGroup, int unitsNeeded,
                           Urgency urgency, String note, Instant createdAt, RequestStatus requestStatus,
                           ResponseStatus myResponse, Double distanceKm) {}

    public record SelfReport(@NotNull LocalDate date, String place, String note) {}

    public record DonationDto(Long id, LocalDate date, String place, int units, DonationStatus status,
                              String note, String donorName) {}

    // ---- search ----
    public record DonorSearchResult(Long id, String name, BloodGroup bloodGroup, boolean exactMatch, String city,
                                    Double distanceKm, double latitude, double longitude, boolean verifiedDonor,
                                    String phone) {}

    // ---- hospital / requests ----
    public record CreateRequest(@NotNull BloodGroup bloodGroup, @Min(1) @Max(20) int unitsNeeded, @NotNull Urgency urgency,
                                @Size(max = 500) String note, Double latitude, Double longitude,
                                @Min(1) @Max(200) Integer radiusKm, LocalDateTime neededBy) {}

    public record RequestDto(Long id, BloodGroup bloodGroup, int unitsNeeded, Urgency urgency, String note, int radiusKm,
                             RequestStatus status, Instant createdAt, LocalDateTime neededBy, String hospitalName,
                             long notified, long accepted, long declined) {}

    public record ResponseDto(Long donorId, String name, BloodGroup bloodGroup, Double distanceKm,
                              ResponseStatus status, boolean verifiedDonor, String phone, Instant respondedAt) {}

    public record RecordDonation(Long donorId, String donorEmail, Long requestId,
                                 @Min(1) @Max(3) Integer units, LocalDate date, String note) {}

    public record StatusChange(@NotNull RequestStatus status) {}

    public record HospitalDto(Long id, String name, String registrationNo, String address, String city,
                              VerificationStatus status, String rejectionReason, String contactName,
                              String contactEmail, String contactPhone, Instant createdAt) {}

    public record Reject(@NotBlank String reason) {}

    public record NotificationDto(Long id, String title, String message, Long requestId, boolean seen, Instant createdAt) {}
}
