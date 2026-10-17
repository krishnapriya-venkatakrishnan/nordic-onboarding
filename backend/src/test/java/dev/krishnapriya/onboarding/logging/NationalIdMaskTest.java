package dev.krishnapriya.onboarding.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NationalIdMaskTest {

    /** Every shape a Swedish or Danish number can be typed in, including coordination numbers. */
    @ParameterizedTest
    @ValueSource(strings = {
            "19121212-1212", "191212121212", "121212-1212", "121212+1212", "1212121212",
            "811278-9873", "811218 9876", "121285-1234", "1212851234"})
    void masksEverySwedishAndDanishShape(String id) {
        assertEquals("value=[national ID masked].", NationalIdMask.scrub("value=" + id + "."));
    }

    @Test
    void masksEveryOccurrence() {
        assertEquals("[national ID masked] and [national ID masked]",
                NationalIdMask.scrub("19121212-1212 and 121285-1234"));
    }

    @Test
    void leavesOtherNumbersAlone() {
        String text = "APP-1234ABCD: cap 174200 DKK for 2026, revision 00004, age 18, port 8080";
        assertEquals(text, NationalIdMask.scrub(text));
    }

    @Test
    void passesNullThrough() {
        assertNull(NationalIdMask.scrub(null));
    }
}
