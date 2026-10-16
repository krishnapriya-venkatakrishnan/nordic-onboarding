package dev.krishnapriya.onboarding.domain;

import java.time.LocalDate;

/** Outcome of parsing a national ID. Either valid (with normalized form and birth date) or invalid (with a reason). */
public record NationalIdResult(boolean valid, String normalized, LocalDate birthDate, String error) {

    public static NationalIdResult ok(String normalized, LocalDate birthDate) {
        return new NationalIdResult(true, normalized, birthDate, null);
    }

    public static NationalIdResult invalid(String error) {
        return new NationalIdResult(false, null, null, error);
    }

    /** The generated record toString would print the ID number, so it could leak into logs and error traces. */
    @Override
    public String toString() {
        return valid ? "NationalIdResult[valid]" : "NationalIdResult[invalid: " + error + "]";
    }
}
