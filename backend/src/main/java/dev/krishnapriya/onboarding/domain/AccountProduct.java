package dev.krishnapriya.onboarding.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * A savings/investment account type a market offers.
 *
 * @param annualDepositCaps deposit cap per calendar year, in the market's currency.
 *                          Empty map means no cap. Caps are data, so a rule change
 *                          (e.g. a new year's cap) is a config change, not a code change.
 * @param onePerPerson      whether a person may hold only one account of this type
 */
public record AccountProduct(
        String id,
        String name,
        String summary,
        Map<Integer, BigDecimal> annualDepositCaps,
        boolean onePerPerson) {

    public AccountProduct {
        annualDepositCaps = Map.copyOf(annualDepositCaps);
    }

    /** The cap for a given year: the exact year if known, otherwise the latest earlier year. */
    public Optional<BigDecimal> depositCapFor(int year) {
        return annualDepositCaps.entrySet().stream()
                .filter(e -> e.getKey() <= year)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue);
    }
}
