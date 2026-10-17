package dev.krishnapriya.onboarding.markets.se;

import dev.krishnapriya.onboarding.domain.NationalIdResult;
import dev.krishnapriya.onboarding.domain.RejectionReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonnummerRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private final PersonnummerRule rule = new PersonnummerRule();

    @Test
    void acceptsTwelveDigitFormWithAndWithoutHyphen() {
        NationalIdResult withHyphen = rule.parse("19811218-9876", TODAY);
        NationalIdResult without = rule.parse("198112189876", TODAY);

        assertTrue(withHyphen.valid());
        assertTrue(without.valid());
        assertEquals("19811218-9876", without.normalized());
        assertEquals(LocalDate.of(1981, 12, 18), withHyphen.birthDate());
    }

    @Test
    void resolvesCenturyForTenDigitForm() {
        assertEquals(LocalDate.of(1981, 12, 18), rule.parse("811218-9876", TODAY).birthDate());
        assertEquals(LocalDate.of(2012, 12, 12), rule.parse("121212-1212", TODAY).birthDate());
    }

    /** The century depends on the whole date: a birthday later this year can only be a hundred years ago. */
    @Test
    void tenDigitFormBornLaterThisYearIsFromLastCentury() {
        NationalIdResult result = rule.parse("261231-1239", TODAY);
        assertTrue(result.valid(), result.error());
        assertEquals(LocalDate.of(1926, 12, 31), result.birthDate());
    }

    @Test
    void plusSeparatorMeansHundredOrOlder() {
        NationalIdResult result = rule.parse("121212+1212", TODAY);
        assertTrue(result.valid());
        assertEquals(LocalDate.of(1912, 12, 12), result.birthDate());
        assertEquals("19121212-1212", result.normalized());
    }

    @Test
    void acceptsCoordinationNumbers() {
        NationalIdResult result = rule.parse("811278-9873", TODAY);
        assertTrue(result.valid());
        assertEquals(LocalDate.of(1981, 12, 18), result.birthDate());
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertFalse(rule.parse("19811218-9875", TODAY).valid());
        assertEquals(RejectionReason.ID_CHECK_DIGIT, rule.parse("19811218-9875", TODAY).reason());
    }

    @Test
    void rejectsImpossibleDateEvenWithValidCheckDigit() {
        NationalIdResult result = rule.parse("19810230-1234", TODAY);
        assertFalse(result.valid());
        assertEquals("The date part isn't a real date.", result.error());
        assertEquals(RejectionReason.ID_INVALID_DATE, result.reason());
    }

    @Test
    void rejectsBirthDateInTheFuture() {
        NationalIdResult result = rule.parse("20261231-1239", TODAY);
        assertFalse(result.valid());
        assertEquals("The date part is in the future.", result.error());
        assertEquals(RejectionReason.ID_FUTURE_DATE, result.reason());
    }

    @Test
    void rejectsBadFormat() {
        assertFalse(rule.parse("1981-12-18", TODAY).valid());
        assertEquals(RejectionReason.ID_FORMAT, rule.parse("1981-12-18", TODAY).reason());
        assertFalse(rule.parse("", TODAY).valid());
    }
}
