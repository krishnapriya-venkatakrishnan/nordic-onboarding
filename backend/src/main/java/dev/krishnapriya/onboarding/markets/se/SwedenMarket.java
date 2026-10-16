package dev.krishnapriya.onboarding.markets.se;

import dev.krishnapriya.onboarding.domain.AccountProduct;
import dev.krishnapriya.onboarding.domain.Disclosure;
import dev.krishnapriya.onboarding.domain.IdentityProvider;
import dev.krishnapriya.onboarding.domain.Market;
import dev.krishnapriya.onboarding.domain.NationalIdRule;

import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Sweden: the home market. Product and disclosure wording is illustrative, not legal text. */
public final class SwedenMarket implements Market {

    private final NationalIdRule nationalId = new PersonnummerRule();

    @Override
    public String code() {
        return "SE";
    }

    @Override
    public String displayName() {
        return "Sweden";
    }

    @Override
    public Locale locale() {
        return Locale.forLanguageTag("sv-SE");
    }

    @Override
    public Currency currency() {
        return Currency.getInstance("SEK");
    }

    @Override
    public IdentityProvider identityProvider() {
        return new IdentityProvider("bankid", "BankID");
    }

    @Override
    public NationalIdRule nationalIdRule() {
        return nationalId;
    }

    @Override
    public List<AccountProduct> products() {
        return List.of(
                new AccountProduct("isk", "Investeringssparkonto (ISK)",
                        "Taxed at a flat standard rate on the account's value, not on each sale or dividend. No deposit cap.",
                        Map.of(), false),
                new AccountProduct("af", "Aktie- och fondkonto",
                        "A standard account. Gains and dividends are taxed as they happen and reported on your tax return.",
                        Map.of(), false));
    }

    @Override
    public List<Disclosure> disclosures() {
        return List.of(
                Disclosure.required("terms", "I have read and accept the account terms and conditions."),
                Disclosure.required("kyc",
                        "The information I give is correct, and I will answer the customer due diligence (kundkännedom) questions."),
                Disclosure.optional("news", "Send me occasional product news by email."));
    }
}
