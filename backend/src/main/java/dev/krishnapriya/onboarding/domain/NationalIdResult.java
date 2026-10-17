package dev.krishnapriya.onboarding.domain;

import java.time.LocalDate;

/**
 * Outcome of parsing a national ID. Either valid (with normalized form and birth date) or invalid
 * (with a reason code for counting and a message for the person).
 */
public record NationalIdResult(boolean valid, String normalized, LocalDate birthDate,
                               RejectionReason reason, String error) {

    public static NationalIdResult ok(String normalized, LocalDate birthDate) {
        return new NationalIdResult(true, normalized, birthDate, null, null);
    }

    public static NationalIdResult invalid(RejectionReason reason, String error) {
        return new NationalIdResult(false, null, null, reason, error);
    }

    /** The generated record toString would print the ID number, so it could leak into logs and error traces. */
    @Override
    public String toString() {
        return valid ? "NationalIdResult[valid]" : "NationalIdResult[invalid " + reason + ": " + error + "]";
    }
}
