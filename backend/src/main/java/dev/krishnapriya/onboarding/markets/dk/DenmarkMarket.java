package dev.krishnapriya.onboarding.markets.dk;

import dev.krishnapriya.onboarding.domain.AccountProduct;
import dev.krishnapriya.onboarding.domain.Disclosure;
import dev.krishnapriya.onboarding.domain.IdentityProvider;
import dev.krishnapriya.onboarding.domain.Market;
import dev.krishnapriya.onboarding.domain.NationalIdRule;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Denmark: the new market. Product and disclosure wording is illustrative, not legal text.
 *
 * Aktiesparekonto deposit caps are per Skattestyrelsen (skat.dk): 166,200 DKK for 2025
 * and 174,200 DKK for 2026. A proposal to raise the cap to 500,000 DKK was not enacted
 * at the time of writing. If it passes, it is one new map entry here, not a code change.
 */
public final class DenmarkMarket implements Market {

    static final String AKTIESPAREKONTO = "aktiesparekonto";

    private final NationalIdRule nationalId = new CprRule();

    @Override
    public String code() {
        return "DK";
    }

    @Override
    public String displayName() {
        return "Denmark";
    }

    @Override
    public Locale locale() {
        return Locale.forLanguageTag("da-DK");
    }

    @Override
    public ZoneId zone() {
        return ZoneId.of("Europe/Copenhagen");
    }

    @Override
    public Currency currency() {
        return Currency.getInstance("DKK");
    }

    @Override
    public IdentityProvider identityProvider() {
        return new IdentityProvider("mitid", "MitID");
    }

    @Override
    public NationalIdRule nationalIdRule() {
        return nationalId;
    }

    @Override
    public List<AccountProduct> products() {
        return List.of(
                new AccountProduct(AKTIESPAREKONTO, "Aktiesparekonto",
                        "Returns taxed at 17% each year. One per person, with an annual deposit cap.",
                        Map.of(2025, new BigDecimal("166200"), 2026, new BigDecimal("174200")), true),
                new AccountProduct("depot", "Depot (frie midler)",
                        "A standard investment account with no deposit cap. Returns are taxed as share income.",
                        Map.of(), false));
    }

    @Override
    public List<Disclosure> disclosures() {
        return List.of(
                Disclosure.required("terms", "I have read and accept the account terms and conditions."),
                Disclosure.required("kyc",
                        "The information I give is correct, and I will answer the customer due diligence questions."),
                Disclosure.requiredFor(AKTIESPAREKONTO, "ask-one-per-person",
                        "I don't already hold an Aktiesparekonto with another bank."),
                Disclosure.requiredFor(AKTIESPAREKONTO, "ask-tax-liable",
                        "I am fully tax liable to Denmark."),
                Disclosure.optional("news", "Send me occasional product news by email."));
    }
}
