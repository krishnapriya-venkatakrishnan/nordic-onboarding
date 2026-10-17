package dev.krishnapriya.onboarding.domain;

import dev.krishnapriya.onboarding.domain.OnboardingEvent.Outcome;
import dev.krishnapriya.onboarding.markets.dk.DenmarkMarket;
import dev.krishnapriya.onboarding.markets.se.SwedenMarket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingServiceTest {

    private final List<OnboardingEvent> events = new ArrayList<>();
    private final OnboardingService service = serviceOn(LocalDate.of(2026, 10, 15), events::add);

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

    /** 22:30 UTC on 14 October is already 00:30 on the 15th in Copenhagen: the customer's 18th birthday. */
    @Test
    void birthdaysTurnOverAtTheMarketsMidnight() {
        OnboardingService justAfterMidnightInDenmark = serviceAt(Instant.parse("2026-10-14T22:30:00Z"));
        assertTrue(justAfterMidnightInDenmark.checkNationalId("DK", "151008-4000").isEmpty());
    }

    /** 23:30 UTC on 31 December is already the new year in Copenhagen, so the new cap applies. */
    @Test
    void depositCapYearTurnsOverAtTheMarketsMidnight() {
        assertEquals(new BigDecimal("174200"), capOf(serviceAt(Instant.parse("2025-12-31T23:30:00Z"))));
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
        assertEquals(List.of(RejectionReason.IDENTITY_NOT_VERIFIED, RejectionReason.ID_FORMAT,
                RejectionReason.NAME_MISSING, RejectionReason.EMAIL_INVALID, RejectionReason.PRODUCT_NOT_OFFERED,
                RejectionReason.CONFIRMATION_MISSING, RejectionReason.CONFIRMATION_MISSING),
                result.errors().stream().map(FieldError::code).toList());
        assertNull(result.applicationId());
    }

    @Test
    void recordsAVerifiedLoginForItsMarket() {
        service.startIdentitySession("dk");
        assertEquals(List.of(event("DK", StepType.IDENTITY, Outcome.VERIFIED, null)), events);
    }

    @Test
    void recordsWhyAnIdNumberWasRejected() {
        service.checkNationalId("SE", "19811218-9875");
        service.checkNationalId("DK", "010115-4123");
        service.checkNationalId("DK", "12345");
        service.checkNationalId("SE", "19811218-9876"); // valid: nothing to record

        assertEquals(List.of(
                event("SE", StepType.PERSONAL_DETAILS, Outcome.ID_REJECTED, RejectionReason.ID_CHECK_DIGIT),
                event("DK", StepType.PERSONAL_DETAILS, Outcome.ID_REJECTED, RejectionReason.ID_UNDER_AGE),
                event("DK", StepType.PERSONAL_DETAILS, Outcome.ID_REJECTED, RejectionReason.ID_FORMAT)), events);
    }

    @Test
    void recordsEverySubmissionAndItsAcceptance() {
        service.submit("DK", danishRequest("depot", Set.of("terms", "kyc")));

        assertEquals(List.of(
                event("DK", StepType.IDENTITY, Outcome.VERIFIED, null),
                event("DK", StepType.REVIEW, Outcome.SUBMITTED, null),
                event("DK", StepType.REVIEW, Outcome.ACCEPTED, null)), events);
    }

    /** One reason per rejection: the first problem in flow order, so it can be counted cleanly. */
    @Test
    void recordsTheFirstReasonARejectedApplicationStopped() {
        service.submit("SE", new ApplicationRequest(null, "123", "", "not-an-email", null, Set.of()));
        service.submit("DK", danishRequest("aktiesparekonto", Set.of("terms", "kyc")));

        List<OnboardingEvent> rejected = events.stream().filter(e -> e.outcome() == Outcome.REJECTED).toList();
        assertEquals(List.of(
                event("SE", StepType.REVIEW, Outcome.REJECTED, RejectionReason.IDENTITY_NOT_VERIFIED),
                event("DK", StepType.REVIEW, Outcome.REJECTED, RejectionReason.CONFIRMATION_MISSING)), rejected);
        assertEquals(2, events.stream().filter(e -> e.outcome() == Outcome.SUBMITTED).count());
        // The ID check inside a submission is part of that rejection, not a separate ID_REJECTED.
        assertTrue(events.stream().noneMatch(e -> e.outcome() == Outcome.ID_REJECTED));
    }

    private static OnboardingEvent event(String market, StepType step, Outcome outcome, RejectionReason reason) {
        return new OnboardingEvent(market, step, outcome, reason);
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

    /** Midday UTC, so the date is the same in every market's time zone. */
    private static OnboardingService serviceOn(LocalDate date) {
        return serviceOn(date, event -> { });
    }

    private static OnboardingService serviceOn(LocalDate date, OnboardingEvents events) {
        return serviceAt(date.atTime(12, 0).toInstant(ZoneOffset.UTC), events);
    }

    private static OnboardingService serviceAt(Instant instant) {
        return serviceAt(instant, event -> { });
    }

    private static OnboardingService serviceAt(Instant instant, OnboardingEvents events) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        MarketRegistry registry = new MarketRegistry(List.of(new SwedenMarket(), new DenmarkMarket()), Set.of("SE", "DK"));
        return new OnboardingService(registry, clock, events);
    }
}
