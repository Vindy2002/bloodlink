package lk.bloodlink;

import lk.bloodlink.config.AppProperties;
import lk.bloodlink.domain.*;
import lk.bloodlink.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

/** Fills an empty database with an admin, hospitals and ~60 donors around Sri Lanka for demos. */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DonorProfileRepository donors;
    private final HospitalRepository hospitals;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    private static final Object[][] CITIES = {
            {"Colombo", "Colombo", 6.9271, 79.8612}, {"Kandy", "Kandy", 7.2906, 80.6337},
            {"Galle", "Galle", 6.0535, 80.2210}, {"Kurunegala", "Kurunegala", 7.4863, 80.3647},
            {"Jaffna", "Jaffna", 9.6615, 80.0255}, {"Anuradhapura", "Anuradhapura", 8.3114, 80.4037},
            {"Matara", "Matara", 5.9549, 80.5550}, {"Negombo", "Gampaha", 7.2008, 79.8737},
            {"Ratnapura", "Ratnapura", 6.6828, 80.3992}, {"Badulla", "Badulla", 6.9934, 81.0550},
            {"Kegalle", "Kegalle", 7.2513, 80.3464}, {"Chilaw", "Puttalam", 7.5758, 79.7953},
            {"Matale", "Matale", 7.4675, 80.6234}, {"Gampaha", "Gampaha", 7.0873, 79.9925}};
    private static final BloodGroup[] POOL = {
            BloodGroup.O_POS, BloodGroup.O_POS, BloodGroup.O_POS, BloodGroup.A_POS, BloodGroup.A_POS, BloodGroup.A_POS,
            BloodGroup.B_POS, BloodGroup.B_POS, BloodGroup.AB_POS, BloodGroup.O_NEG, BloodGroup.A_NEG,
            BloodGroup.B_NEG, BloodGroup.AB_NEG};
    private static final String[] FIRST = {"Nimal", "Kamal", "Sunil", "Nuwan", "Chamari", "Dilani", "Ishara", "Kasun", "Thilini", "Ruwan", "Sanduni", "Pradeep", "Anusha", "Lakmal", "Madhavi"};
    private static final String[] LAST = {"Perera", "Silva", "Fernando", "Jayasinghe", "Bandara", "Wickramasinghe", "Rajapaksa", "Gunawardena", "Herath", "Dissanayake"};

    @Override
    public void run(String... args) {
        if (!props.seedDemoData() || users.count() > 0) return;

        user("admin@bloodlink.lk", "Admin@123", "BloodLink Admin", "0770000000", Role.ADMIN);

        hospital("hospital@kurunegala.lk", "Teaching Hospital Kurunegala", "KRG-001", "Kurunegala", 7.4863, 80.3647, VerificationStatus.VERIFIED);
        hospital("hospital@colombo.lk", "National Hospital Colombo", "CMB-001", "Colombo", 6.9271, 79.8612, VerificationStatus.VERIFIED);
        hospital("pending@kandy.lk", "Kandy Care Hospital", "KDY-044", "Kandy", 7.2906, 80.6337, VerificationStatus.PENDING);

        Random rnd = new Random(42);
        for (int i = 1; i <= 60; i++) {
            Object[] c = CITIES[i == 1 ? 3 : rnd.nextInt(CITIES.length)];
            BloodGroup bg = i == 1 ? BloodGroup.A_POS : POOL[rnd.nextInt(POOL.length)];
            User u = user("donor" + i + "@demo.lk", "Donor@123",
                    FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)],
                    "07" + (1 + rnd.nextInt(8)) + String.format("%07d", rnd.nextInt(10_000_000)), Role.DONOR);
            DonorProfile d = new DonorProfile();
            d.setUser(u);
            d.setBloodGroup(bg);
            d.setCity((String) c[0]);
            d.setDistrict((String) c[1]);
            d.setLatitude((double) c[2] + (rnd.nextDouble() - 0.5) * 0.12);   // spread donors within ~6 km of the city
            d.setLongitude((double) c[3] + (rnd.nextDouble() - 0.5) * 0.12);
            d.setGender(rnd.nextBoolean() ? "Male" : "Female");
            d.setWeightKg(52 + rnd.nextInt(40));
            d.setDateOfBirth(LocalDate.now().minusYears(20 + rnd.nextInt(30)));
            d.setShowPhone(rnd.nextInt(3) == 0);
            if (i > 2 && rnd.nextInt(6) == 0) d.setLastDonationDate(LocalDate.now().minusDays(10 + rnd.nextInt(60))); // not yet eligible
            else if (rnd.nextInt(3) == 0) { d.setLastDonationDate(LocalDate.now().minusDays(120 + rnd.nextInt(300))); d.setVerifiedDonor(true); }
            if (i > 2 && rnd.nextInt(10) == 0) d.setAvailable(false);
            donors.save(d);
        }
    }

    private User user(String email, String pw, String name, String phone, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pw));
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        return users.save(u);
    }

    private void hospital(String email, String name, String reg, String city, double lat, double lng, VerificationStatus st) {
        User u = user(email, "Hospital@123", name + " Blood Bank", "0112000000", Role.HOSPITAL);
        Hospital h = new Hospital();
        h.setUser(u);
        h.setName(name);
        h.setRegistrationNo(reg);
        h.setCity(city);
        h.setAddress(city);
        h.setLatitude(lat);
        h.setLongitude(lng);
        h.setStatus(st);
        hospitals.save(h);
    }
}
