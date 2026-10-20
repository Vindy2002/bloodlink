package lk.bloodlink.service;

import lk.bloodlink.config.JwtService;
import lk.bloodlink.domain.*;
import lk.bloodlink.repo.*;
import lk.bloodlink.web.ApiException;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final DonorProfileRepository donors;
    private final HospitalRepository hospitals;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final NotificationService notifier;

    @Transactional
    public AuthResponse registerDonor(DonorRegistration r) {
        boolean regular = Boolean.TRUE.equals(r.regularDonor());
        List<String> problems = Eligibility.problems(r.dateOfBirth(), r.weightKg(), regular, r.conditions());
        if (!problems.isEmpty())
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "You cannot register as a donor right now. " + String.join(" ", problems));
        User u = newUser(r.email(), r.password(), r.fullName(), r.phone(), Role.DONOR);
        DonorProfile d = new DonorProfile();
        d.setUser(u);
        d.setBloodGroup(r.bloodGroup());
        d.setGender(r.gender());
        d.setDateOfBirth(r.dateOfBirth());
        d.setWeightKg(r.weightKg());
        d.setRegularDonor(regular);
        d.setCity(r.city());
        d.setDistrict(r.district());
        d.setLatitude(r.latitude());
        d.setLongitude(r.longitude());
        donors.save(d);
        return respond(u);
    }

    @Transactional
    public AuthResponse registerHospital(HospitalRegistration r) {
        User u = newUser(r.email(), r.password(), r.contactName(), r.phone(), Role.HOSPITAL);
        Hospital h = new Hospital();
        h.setUser(u);
        h.setName(r.hospitalName());
        h.setRegistrationNo(r.registrationNo());
        h.setAddress(r.address());
        h.setCity(r.city());
        h.setLatitude(r.latitude());
        h.setLongitude(r.longitude());
        hospitals.save(h);
        users.findByRole(Role.ADMIN).forEach(a ->
                notifier.inApp(a, "New hospital awaiting verification", h.getName() + " (" + h.getCity() + ") registered.", null));
        return respond(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password."));
        return respond(u);
    }

    public UserInfo info(User u) {
        VerificationStatus hs = u.getRole() == Role.HOSPITAL
                ? hospitals.findByUserId(u.getId()).map(Hospital::getStatus).orElse(null) : null;
        return new UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), hs);
    }

    private AuthResponse respond(User u) {
        return new AuthResponse(jwt.generate(u), info(u));
    }

    private User newUser(String email, String password, String name, String phone, Role role) {
        if (users.existsByEmailIgnoreCase(email))
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        return users.save(u);
    }
}
