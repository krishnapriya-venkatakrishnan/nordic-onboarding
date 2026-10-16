package dev.krishnapriya.onboarding.domain;

import java.time.LocalDate;

/**
 * Parses and validates a market's national identity number.
 *
 * Implementations only answer "is this a well-formed number, and what birth date
 * does it encode?". Policy such as minimum age is applied by the core service, so
 * it stays the same in every market.
 */
public interface NationalIdRule {

    /** Human name, e.g. "Personnummer" or "CPR number". */
    String label();

    /** Example shown in the input, e.g. "YYYYMMDD-NNNN". */
    String placeholder();

    /** A well-known test number to show in the demo. Never a real person's number. */
    String example();

    /** Loose regex for instant client-side feedback. The server check is authoritative. */
    String formatPattern();

    /**
     * @param raw   what the user typed
     * @param today the reference date, injected so century and age logic is testable
     */
    NationalIdResult parse(String raw, LocalDate today);
}
