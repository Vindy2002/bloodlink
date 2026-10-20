package lk.bloodlink.domain;

import java.util.List;

public enum BloodGroup {
    A_POS("A+"), A_NEG("A-"), B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"), O_POS("O+"), O_NEG("O-");

    private final String label;
    BloodGroup(String label) { this.label = label; }
    public String getLabel() { return label; }

    /** Donor groups that can safely give red cells to a recipient of THIS group. */
    public List<BloodGroup> donorsFor() {
        return switch (this) {
            case O_NEG -> List.of(O_NEG);
            case O_POS -> List.of(O_POS, O_NEG);
            case A_NEG -> List.of(A_NEG, O_NEG);
            case A_POS -> List.of(A_POS, A_NEG, O_POS, O_NEG);
            case B_NEG -> List.of(B_NEG, O_NEG);
            case B_POS -> List.of(B_POS, B_NEG, O_POS, O_NEG);
            case AB_NEG -> List.of(AB_NEG, A_NEG, B_NEG, O_NEG);
            case AB_POS -> List.of(values());
        };
    }
}
