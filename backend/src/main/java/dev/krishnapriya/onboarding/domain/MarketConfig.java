package dev.krishnapriya.onboarding.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the UI needs to render a market's onboarding flow. The frontend has no
 * country-specific code: it draws whatever this describes.
 */
public record MarketConfig(
        String code,
        String displayName,
        String locale,
        String currency,
        IdentityProvider identityProvider,
        NationalIdField nationalId,
        int minimumAge,
        List<StepType> steps,
        List<Product> products,
        List<Disclosure> disclosures) {

    public record NationalIdField(String label, String placeholder, String pattern, String example) {
    }

    /** A product as shown to the customer, with this year's deposit cap resolved (null = no cap). */
    public record Product(String id, String name, String summary, BigDecimal depositCap, boolean onePerPerson) {
    }

    public record Summary(String code, String displayName, String identityProvider) {
    }
}
