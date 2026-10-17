package dev.krishnapriya.onboarding.logging;

import java.util.regex.Pattern;

/**
 * Rule 7's safety net: replaces anything shaped like a Swedish or Danish ID number before it is
 * written. The code never logs ID numbers on purpose; this catches accidents, such as an ID
 * inside an exception message.
 *
 * It deliberately errs towards masking: an unrelated 10- or 12-digit number is masked too.
 */
public final class NationalIdMask {

    static final String MASK = "[national ID masked]";

    // 6 or 8 digits (YYMMDD, YYYYMMDD, DDMMYY), an optional '-', '+' or space, then 4 digits,
    // with no digit directly before or after.
    private static final Pattern ID = Pattern.compile("(?<!\\d)(?:\\d{2})?\\d{6}[-+ ]?\\d{4}(?!\\d)");

    private NationalIdMask() {
    }

    public static String scrub(String text) {
        return text == null ? null : ID.matcher(text).replaceAll(MASK);
    }
}
