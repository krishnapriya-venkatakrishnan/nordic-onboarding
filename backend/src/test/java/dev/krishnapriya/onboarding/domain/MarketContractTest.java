package dev.krishnapriya.onboarding.domain;

import dev.krishnapriya.onboarding.markets.dk.DenmarkMarket;
import dev.krishnapriya.onboarding.markets.se.SwedenMarket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Rules every market must follow. A new market is added to MARKETS and gets this
 * whole suite for free, which is what makes launching market N+1 low-risk.
 * Each check runs once per market, so one market failing never hides another.
 */
class MarketContractTest {

    static final List<Market> MARKETS = List.of(new SwedenMarket(), new DenmarkMarket());

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    /** Names each run after the market code, e.g. "[2] DK". */
    static Stream<Arguments> markets() {
        return MARKETS.stream().map(m -> argumentSet(m.code(), m));
    }

    @Test
    void codesAreUniqueTwoLetterCountryCodes() {
        Set<String> seen = new HashSet<>();
        for (Market m : MARKETS) {
            assertTrue(m.code().matches("[A-Z]{2}"), m.code());
            assertTrue(seen.add(m.code()), "duplicate " + m.code());
        }
    }

    @ParameterizedTest
    @MethodSource("markets")
    void flowStartsWithIdentityAndEndsWithReview(Market m) {
        assertEquals(StepType.IDENTITY, m.steps().getFirst());
        assertEquals(StepType.REVIEW, m.steps().getLast());
    }

    @ParameterizedTest
    @MethodSource("markets")
    void offersAtLeastOneProductWithUniqueIds(Market m) {
        assertFalse(m.products().isEmpty());
        assertEquals(m.products().size(), m.products().stream().map(AccountProduct::id).distinct().count());
    }

    @ParameterizedTest
    @MethodSource("markets")
    void requiresTermsAcceptance(Market m) {
        assertTrue(m.disclosures().stream().anyMatch(d -> d.id().equals("terms") && d.required()));
    }

    /** Accepted confirmations are sent back by id, so two with the same id could not be told apart. */
    @ParameterizedTest
    @MethodSource("markets")
    void disclosureIdsAreUnique(Market m) {
        assertEquals(m.disclosures().size(), m.disclosures().stream().map(Disclosure::id).distinct().count());
    }

    @ParameterizedTest
    @MethodSource("markets")
    void productScopedDisclosuresPointAtRealProducts(Market m) {
        Set<String> productIds = m.products().stream().map(AccountProduct::id).collect(Collectors.toSet());
        m.disclosures().stream().filter(d -> d.productId() != null)
                .forEach(d -> assertTrue(productIds.contains(d.productId()), d.id()));
    }

    @ParameterizedTest
    @MethodSource("markets")
    void nationalIdPlaceholderAndPatternAreConsistent(Market m) {
        NationalIdRule rule = m.nationalIdRule();
        assertFalse(rule.label().isBlank());
        assertFalse(rule.placeholder().isBlank());
        Pattern pattern = Pattern.compile(rule.formatPattern()); // must be a valid regex for the UI
        assertTrue(pattern.matcher(rule.example()).matches(), "example vs pattern");
        assertTrue(rule.parse(rule.example(), TODAY).valid(), "example must be valid");
        // Garbage must never validate in any market.
        assertFalse(rule.parse("not-an-id", TODAY).valid());
    }
}
