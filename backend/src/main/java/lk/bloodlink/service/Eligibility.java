package lk.bloodlink.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/** Donor eligibility rules (age, weight, health declaration). Hemoglobin (12.5 g/dl) is tested at the blood bank on the day. */
public final class Eligibility {
    private Eligibility() {}

    public static final Map<String, String> LONG_TERM = new LinkedHashMap<>();
    public static final Map<String, String> TEMPORARY = new LinkedHashMap<>();
    static {
        LONG_TERM.put("BLOODBORNE", "blood-borne infection (HIV/AIDS, Hepatitis B or C, Syphilis)");
        LONG_TERM.put("DIABETES", "diabetes");
        LONG_TERM.put("BLOOD_PRESSURE", "high blood pressure");
        LONG_TERM.put("HEART", "heart disease");
        LONG_TERM.put("ASTHMA", "asthma");
        LONG_TERM.put("KIDNEY", "kidney disease");
        LONG_TERM.put("LIVER", "liver disease");
        LONG_TERM.put("EPILEPSY", "epilepsy / fits");

        TEMPORARY.put("PREGNANT_OR_BREASTFEEDING", "pregnant or breastfeeding");
        TEMPORARY.put("TATTOO_PIERCING_12M", "tattoo, ear or body piercing in the last 12 months");
        TEMPORARY.put("RECENT_SURGERY", "recent surgery");
        TEMPORARY.put("ON_MEDICATION", "taking antibiotics or other special medicines");
        TEMPORARY.put("FEVER_OR_INFECTION", "fever, cold or other infection (or only recently recovered)");
        TEMPORARY.put("RECENT_VACCINE", "recently had a vaccine");
        TEMPORARY.put("NOT_WELL", "not in good physical or mental health");
    }

    public static int ageOf(LocalDate dob) { return Period.between(dob, LocalDate.now()).getYears(); }

    /** 18-55; up to 60 only for people who have donated regularly before. */
    public static List<String> ageProblems(LocalDate dob, boolean regularDonor) {
        List<String> p = new ArrayList<>();
        if (dob == null) { p.add("Date of birth is required."); return p; }
        int age = ageOf(dob);
        if (age < 18) p.add("You must be at least 18 years old to donate.");
        else if (age > 60) p.add("Blood donation is not possible above 60 years.");
        else if (age > 55 && !regularDonor) p.add("Above 55, only people who have donated regularly before can donate (up to 60).");
        return p;
    }

    public static List<String> problems(LocalDate dob, Integer weightKg, boolean regularDonor, Collection<String> conditions) {
        List<String> p = ageProblems(dob, regularDonor);
        if (weightKg == null || weightKg <= 50) p.add("Your weight must be more than 50 kg.");
        if (conditions != null) {
            for (String c : conditions) {
                if (LONG_TERM.containsKey(c))
                    p.add("Not eligible: " + LONG_TERM.get(c) + ". If it is well controlled, ask the doctor at a blood donation centre.");
                else if (TEMPORARY.containsKey(c))
                    p.add("Not right now: " + TEMPORARY.get(c) + ". Please register once you are clear and the waiting period is over.");
            }
        }
        return p;
    }
}
