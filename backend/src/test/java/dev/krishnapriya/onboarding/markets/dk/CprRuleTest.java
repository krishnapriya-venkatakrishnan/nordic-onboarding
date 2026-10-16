package dev.krishnapriya.onboarding.markets.dk;

import dev.krishnapriya.onboarding.domain.NationalIdResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CprRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private final CprRule rule = new CprRule();

    @Test
    void parsesDateAndNormalizes() {
        NationalIdResult result = rule.parse("1212851234", TODAY);
        assertTrue(result.valid());
        assertEquals("121285-1234", result.normalized());
        assertEquals(LocalDate.of(1985, 12, 12), result.birthDate());
    }

    @Test
    void usesSeventhDigitForCentury() {
        assertEquals(LocalDate.of(2005, 1, 1), rule.parse("010105-4123", TODAY).birthDate());
        assertEquals(LocalDate.of(1937, 1, 1), rule.parse("010137-4123", TODAY).birthDate());
        assertEquals(LocalDate.of(1860, 1, 1), rule.parse("010160-5123", TODAY).birthDate());
        assertEquals(LocalDate.of(2000, 1, 1), rule.parse("010100-9123", TODAY).birthDate());
    }

    /** Every row of the CPR century table, at the edges where the century changes (36/37 and 57/58). */
    @ParameterizedTest(name = "7th digit {0}, year {1} -> {2}")
    @CsvSource({
            "0, 00, 1900", "3, 99, 1900",
            "4, 36, 2000", "4, 37, 1900",
            "9, 36, 2000", "9, 37, 1900",
            "5, 57, 2000", "5, 58, 1800",
            "8, 57, 2000", "8, 99, 1800",
    })
    void centuryTable(int seventhDigit, int yy, int century) {
        assertEquals(century, CprRule.century(seventhDigit, yy));
    }

    @Test
    void doesNotApplyModulus11() {
        // Proves the example really fails the old checksum, so accepting it shows the check is gone.
        assertFalse(passesModulus11("1212851234"));
        assertTrue(rule.parse("121285-1234", TODAY).valid());
    }

    @Test
    void rejectsImpossibleDate() {
        assertEquals("The date part isn't a real date.", rule.parse("300285-1234", TODAY).error());
    }

    @Test
    void rejectsFutureDate() {
        // 7th digit 4 with year 30 means 2030.
        assertEquals("The date part is in the future.", rule.parse("010130-4123", TODAY).error());
    }

    @Test
    void rejectsBadFormat() {
        assertEquals("Use the format DDMMYY-SSSS.", rule.parse("12128-1234", TODAY).error());
        assertEquals("Use the format DDMMYY-SSSS.", rule.parse("abcdef-ghij", TODAY).error());
    }

    /** The checksum Denmark stopped guaranteeing in 2007: weights 4,3,2,7,6,5,4,3,2,1, sum divisible by 11. */
    private static boolean passesModulus11(String tenDigits) {
        int[] weights = {4, 3, 2, 7, 6, 5, 4, 3, 2, 1};
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (tenDigits.charAt(i) - '0') * weights[i];
        }
        return sum % 11 == 0;
    }
}
