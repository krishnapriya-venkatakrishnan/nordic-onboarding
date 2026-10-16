package dev.krishnapriya.onboarding.domain;

import dev.krishnapriya.onboarding.markets.dk.DenmarkMarket;
import dev.krishnapriya.onboarding.markets.se.SwedenMarket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingServiceTest {

    private final OnboardingService service = serviceOn(LocalDate.of(2026, 10, 15));

    @Test
    void depositCapIsResolvedForTheCurrentYear() {
        assertEquals(new BigDecimal("166200"), capOf(serviceOn(LocalDate.of(2025, 6, 1))));
        assertEquals(new BigDecimal("174200"), capOf(serviceOn(LocalDate.of(2026, 6, 1))));
        // No 2027 figure yet: fall back to the latest known cap rather than "no cap".
        assertEquals(new BigDecimal("174200"), capOf(serviceOn(LocalDate.of(2027, 6, 1))));
    }

    @Test
    void uncappedProductHasNullCap() {
        MarketConfig se = service.config("SE");
        assertTrue(se.products().stream().allMatch(p -> p.depositCap() == null));
    }

    @Test
    void opensAktiesparekontoWhenAllRulesAreMet() {
        ApplicationResult result = service.submit("DK", danishRequest("aktiesparekonto",
                Set.of("terms", "kyc", "ask-one-per-person", "ask-tax-liable")));

        assertTrue(result.accepted(), () -> result.errors().toString());
        assertEquals("DKK", result.currency());
    }

    @Test
    void productSpecificDisclosuresOnlyApplyToThatProduct() {
        ApplicationResult ask = service.submit("DK", danishRequest("aktiesparekonto", Set.of("terms", "kyc")));
        ApplicationResult depot = service.submit("DK", danishRequest("depot", Set.of("terms", "kyc")));

        assertFalse(ask.accepted());
        assertEquals(List.of("disclosures.ask-one-per-person", "disclosures.ask-tax-liable"), fields(ask));
        assertTrue(depot.accepted(), () -> depot.errors().toString());
    }

    @Test
    void cannotChooseAProductFromAnotherMarket() {
        ApplicationResult result = service.submit("DK", danishRequest("isk", Set.of("terms", "kyc")));
        assertEquals(List.of("productId"), fields(result));
    }

    @Test
    void identitySessionIsBoundToItsMarket() {
        String swedishSession = service.startIdentitySession("SE");
        ApplicationRequest request = new ApplicationRequest(swedishSession, "121285-1234", "Freja Jensen",
                "freja@example.com", "depot", Set.of("terms", "kyc"));

        assertEquals(List.of("identity"), fields(service.submit("DK", request)));
    }

    /** A successful application uses up the eID session, so one login can't open a second account. */
    @Test
    void identitySessionOpensOnlyOneAccount() {
        ApplicationRequest request = danishRequest("depot", Set.of("terms", "kyc"));

        assertTrue(service.submit("DK", request).accepted());
        assertEquals(List.of("identity"), fields(service.submit("DK", request)));
    }

    /** The clock is fixed at 2026-10-15, so the 18th birthday falls exactly on "today" or "tomorrow". */
    @Test
    void minimumAgeCountsFromTheBirthdayItself() {
        assertTrue(service.checkNationalId("DK", "151008-4000").isEmpty(), "18 today");
        assertTrue(service.checkNationalId("DK", "161008-4000").isPresent(), "18 tomorrow");
    }

    @Test
    void minimumAgeAppliesInEveryMarket() {
        assertEquals("You must be at least 18 to open an account yourself.",
                service.checkNationalId("DK", "010115-4123").orElseThrow().message());
        assertTrue(service.checkNationalId("SE", "20121212-1212").isPresent());
        assertTrue(service.checkNationalId("SE", "19811218-9876").isEmpty());
    }

    @Test
    void reportsEveryProblemAtOnce() {
        ApplicationResult result = service.submit("SE",
                new ApplicationRequest(null, "123", "", "not-an-email", null, Set.of()));

        assertEquals(List.of("identity", "nationalId", "fullName", "email", "productId",
                "disclosures.terms", "disclosures.kyc"), fields(result));
        assertNull(result.applicationId());
    }

    private ApplicationRequest danishRequest(String product, Set<String> accepted) {
        return new ApplicationRequest(service.startIdentitySession("DK"), "121285-1234", "Freja Jensen",
                "freja@example.com", product, accepted);
    }

    private static BigDecimal capOf(OnboardingService s) {
        return s.config("DK").products().stream()
                .filter(p -> p.id().equals("aktiesparekonto")).findFirst().orElseThrow().depositCap();
    }

    private static List<String> fields(ApplicationResult r) {
        return r.errors().stream().map(FieldError::field).toList();
    }

    private static OnboardingService serviceOn(LocalDate date) {
        Clock clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        MarketRegistry registry = new MarketRegistry(List.of(new SwedenMarket(), new DenmarkMarket()), Set.of("SE", "DK"));
        return new OnboardingService(registry, clock);
    }
}
