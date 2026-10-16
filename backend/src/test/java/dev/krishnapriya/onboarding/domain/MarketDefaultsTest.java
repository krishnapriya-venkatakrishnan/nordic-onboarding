package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a market gets unless it says otherwise. */
class MarketDefaultsTest {

    /** Only the required methods, so the defaults are what gets tested. */
    static final Market MINIMAL = new Market() {
        public String code() { return "XX"; }
        public String displayName() { return "Test market"; }
        public Locale locale() { return Locale.ROOT; }
        public ZoneId zone() { return ZoneOffset.UTC; }
        public Currency currency() { return Currency.getInstance("EUR"); }
        public IdentityProvider identityProvider() { return new IdentityProvider("test-eid", "Test eID"); }
        public NationalIdRule nationalIdRule() { return null; }
        public List<AccountProduct> products() { return List.of(); }
        public List<Disclosure> disclosures() { return List.of(); }
    };

    @Test
    void defaultFlowIsIdentityDetailsAccountTypeConfirmationsReview() {
        assertEquals(List.of(StepType.IDENTITY, StepType.PERSONAL_DETAILS, StepType.ACCOUNT_TYPE,
                StepType.DISCLOSURES, StepType.REVIEW), MINIMAL.steps());
    }

    @Test
    void defaultMinimumAgeIs18() {
        assertEquals(18, MINIMAL.minimumAge());
    }
}
