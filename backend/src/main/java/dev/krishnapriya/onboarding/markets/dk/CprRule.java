package dev.krishnapriya.onboarding.markets.dk;

import dev.krishnapriya.onboarding.domain.NationalIdResult;
import dev.krishnapriya.onboarding.domain.NationalIdRule;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Danish CPR number: DDMMYY-SSSS.
 *
 * The century is encoded by the first serial digit together with the year.
 * There is deliberately no modulus-11 check: Denmark stopped guaranteeing it in
 * 2007, so a checksum test would reject real, valid CPR numbers.
 */
public final class CprRule implements NationalIdRule {

    private static final Pattern FORMAT = Pattern.compile("^(\\d{2})(\\d{2})(\\d{2})-?(\\d)(\\d{3})$");

    @Override
    public String label() {
        return "CPR number";
    }

    @Override
    public String placeholder() {
        return "DDMMYY-SSSS";
    }

    /** Fictional, obviously patterned example. */
    @Override
    public String example() {
        return "121285-1234";
    }

    @Override
    public String formatPattern() {
        return "^\\d{6}-?\\d{4}$";
    }

    @Override
    public NationalIdResult parse(String raw, LocalDate today) {
        Matcher m = FORMAT.matcher(raw.replace(" ", ""));
        if (!m.matches()) {
            return NationalIdResult.invalid("Use the format DDMMYY-SSSS.");
        }
        int day = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int yy = Integer.parseInt(m.group(3));
        int centuryDigit = Integer.parseInt(m.group(4));

        int year = century(centuryDigit, yy) + yy;
        LocalDate birthDate;
        try {
            birthDate = LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return NationalIdResult.invalid("The date part isn't a real date.");
        }
        if (birthDate.isAfter(today)) {
            return NationalIdResult.invalid("The date part is in the future.");
        }
        String normalized = m.group(1) + m.group(2) + m.group(3) + "-" + m.group(4) + m.group(5);
        return NationalIdResult.ok(normalized, birthDate);
    }

    /** The CPR century table, keyed on the 7th digit and the two-digit year. */
    static int century(int seventhDigit, int yy) {
        return switch (seventhDigit) {
            case 0, 1, 2, 3 -> 1900;
            case 4, 9 -> yy <= 36 ? 2000 : 1900;
            case 5, 6, 7, 8 -> yy <= 57 ? 2000 : 1800;
            default -> throw new IllegalArgumentException("digit " + seventhDigit);
        };
    }
}
