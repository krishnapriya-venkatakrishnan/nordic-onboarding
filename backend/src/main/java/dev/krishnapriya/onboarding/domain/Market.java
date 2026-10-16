package dev.krishnapriya.onboarding.domain;

import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Everything that differs between markets lives behind this interface.
 *
 * The onboarding flow, API and UI are written once against it. Launching a new
 * market means implementing it (plus its national-ID rule) and enabling the
 * market code in configuration. No core code should ever branch on a market code.
 */
public interface Market {

    /** ISO 3166-1 alpha-2 code, e.g. "SE", "DK". */
    String code();

    String displayName();

    Locale locale();

    /** Where "today" is decided: birthdays and the deposit-cap year turn over at local midnight. */
    ZoneId zone();

    Currency currency();

    IdentityProvider identityProvider();

    NationalIdRule nationalIdRule();

    List<AccountProduct> products();

    List<Disclosure> disclosures();

    /** The onboarding steps, in order. Markets can add, drop or reorder steps. */
    default List<StepType> steps() {
        return List.of(StepType.IDENTITY, StepType.PERSONAL_DETAILS, StepType.ACCOUNT_TYPE,
                StepType.DISCLOSURES, StepType.REVIEW);
    }

    /** Minimum age to open an account on your own. */
    default int minimumAge() {
        return 18;
    }
}
