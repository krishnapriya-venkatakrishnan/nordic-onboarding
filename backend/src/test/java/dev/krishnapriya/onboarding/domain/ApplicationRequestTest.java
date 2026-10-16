package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationRequestTest {

    @Test
    void missingConfirmationsMeanNoneAccepted() {
        ApplicationRequest request = new ApplicationRequest("s", "121285-1234", "Freja Jensen",
                "freja@example.com", "depot", null);
        assertEquals(Set.of(), request.acceptedDisclosures());
    }

    /** Rule 7: an ID number must not reach logs or error traces just because a request got printed. */
    @Test
    void printingARequestHidesPersonalDetails() {
        String printed = new ApplicationRequest("sim-mitid-123", "121285-1234", "Freja Jensen",
                "freja@example.com", "depot", Set.of("terms")).toString();

        assertFalse(printed.contains("121285"), printed);
        assertFalse(printed.contains("Freja"), printed);
        assertFalse(printed.contains("freja@example.com"), printed);
        assertTrue(printed.contains("depot"), printed);
    }
}
