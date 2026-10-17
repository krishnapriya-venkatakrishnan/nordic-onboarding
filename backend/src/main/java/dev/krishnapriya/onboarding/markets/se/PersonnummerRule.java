package dev.krishnapriya.onboarding.markets.se;

import dev.krishnapriya.onboarding.domain.NationalIdResult;
import dev.krishnapriya.onboarding.domain.NationalIdRule;
import dev.krishnapriya.onboarding.domain.RejectionReason;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Swedish personnummer (and samordningsnummer).
 *
 * Accepts YYYYMMDD-NNNN, YYYYMMDDNNNN, YYMMDD-NNNN, YYMMDDNNNN and YYMMDD+NNNN
 * (the "+" marks someone aged 100 or more). The last digit is a Luhn check digit
 * over the ten-digit form. Coordination numbers add 60 to the day.
 */
public final class PersonnummerRule implements NationalIdRule {

    private static final Pattern FORMAT = Pattern.compile("^(\\d{2})?(\\d{2})(\\d{2})(\\d{2})([-+]?)(\\d{3})(\\d)$");

    @Override
    public String label() {
        return "Personnummer";
    }

    @Override
    public String placeholder() {
        return "YYYYMMDD-NNNN";
    }

    /** Skatteverket's long-standing test personnummer. */
    @Override
    public String example() {
        return "19121212-1212";
    }

    @Override
    public String formatPattern() {
        return "^(\\d{2})?\\d{6}[-+]?\\d{4}$";
    }

    @Override
    public NationalIdResult parse(String raw, LocalDate today) {
        String input = raw.replace(" ", "");
        Matcher m = FORMAT.matcher(input);
        if (!m.matches()) {
            return NationalIdResult.invalid(RejectionReason.ID_FORMAT, "Use the format YYYYMMDD-NNNN.");
        }
        String century = m.group(1);
        int yy = Integer.parseInt(m.group(2));
        int month = Integer.parseInt(m.group(3));
        int day = Integer.parseInt(m.group(4));
        boolean centenarian = "+".equals(m.group(5));
        String serial = m.group(6);
        String check = m.group(7);

        String tenDigits = m.group(2) + m.group(3) + m.group(4) + serial + check;
        if (!luhnValid(tenDigits)) {
            return NationalIdResult.invalid(RejectionReason.ID_CHECK_DIGIT,
                    "That personnummer isn't valid. Check the last four digits.");
        }

        // Coordination numbers (samordningsnummer) encode the day as day + 60.
        int birthDay = day > 60 ? day - 60 : day;
        int year = century != null
                ? Integer.parseInt(century) * 100 + yy
                : resolveYear(yy, month, birthDay, centenarian, today);

        LocalDate birthDate;
        try {
            birthDate = LocalDate.of(year, month, birthDay);
        } catch (DateTimeException e) {
            return NationalIdResult.invalid(RejectionReason.ID_INVALID_DATE, "The date part isn't a real date.");
        }
        if (birthDate.isAfter(today)) {
            return NationalIdResult.invalid(RejectionReason.ID_FUTURE_DATE, "The date part is in the future.");
        }
        return NationalIdResult.ok(normalize(year, m.group(3), m.group(4), serial, check), birthDate);
    }

    /** A ten-digit number carries no century: pick the most recent one not in the future. */
    private static int resolveYear(int yy, int month, int day, boolean centenarian, LocalDate today) {
        int year = (today.getYear() / 100) * 100 + yy;
        // Compare the whole date, not just the year: 261231 read on 2026-10-15 is 1926-12-31.
        int candidate = year * 10_000 + month * 100 + day;
        int now = today.getYear() * 10_000 + today.getMonthValue() * 100 + today.getDayOfMonth();
        if (candidate > now) {
            year -= 100;
        }
        if (centenarian) {
            year -= 100;
        }
        return year;
    }

    private static String normalize(int year, String mm, String dd, String serial, String check) {
        return String.format("%04d%s%s-%s%s", year, mm, dd, serial, check);
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(i) - '0';
            if (i % 2 == 0) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return sum % 10 == 0;
    }
}
