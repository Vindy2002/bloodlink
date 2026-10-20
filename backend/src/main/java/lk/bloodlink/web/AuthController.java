package lk.bloodlink.web;

import jakarta.validation.Valid;
import lk.bloodlink.domain.User;
import lk.bloodlink.service.AuthService;
import lk.bloodlink.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/auth/register/donor")
    public AuthResponse registerDonor(@Valid @RequestBody DonorRegistration r) { return auth.registerDonor(r); }

    @PostMapping("/auth/register/hospital")
    public AuthResponse registerHospital(@Valid @RequestBody HospitalRegistration r) { return auth.registerHospital(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal User u) { return auth.info(u); }
}
