package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NationalIdResultTest {

    @Test
    void validResultCarriesTheNormalizedIdAndBirthDate() {
        NationalIdResult ok = NationalIdResult.ok("19121212-1212", LocalDate.of(1912, 12, 12));
        assertTrue(ok.valid());
        assertEquals("19121212-1212", ok.normalized());
        assertEquals(LocalDate.of(1912, 12, 12), ok.birthDate());
    }

    @Test
    void invalidResultCarriesOnlyTheReason() {
        NationalIdResult bad = NationalIdResult.invalid(RejectionReason.ID_FORMAT, "Bad format.");
        assertFalse(bad.valid());
        assertEquals(RejectionReason.ID_FORMAT, bad.reason());
        assertEquals("Bad format.", bad.error());
    }

    /** Rule 7: an ID number must not reach logs or error traces just because a result got printed. */
    @Test
    void printingAResultNeverShowsTheIdNumber() {
        NationalIdResult ok = NationalIdResult.ok("19121212-1212", LocalDate.of(1912, 12, 12));
        assertEquals("NationalIdResult[valid]", ok.toString());
        assertEquals("NationalIdResult[invalid ID_FORMAT: Bad format.]",
                NationalIdResult.invalid(RejectionReason.ID_FORMAT, "Bad format.").toString());
    }
}
