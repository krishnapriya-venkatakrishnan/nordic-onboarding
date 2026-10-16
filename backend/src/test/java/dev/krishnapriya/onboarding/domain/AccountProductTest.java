package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Deposit caps are a dated schedule: each year uses the latest cap set on or before it. */
class AccountProductTest {

    static final AccountProduct CAPPED = new AccountProduct("capped", "Capped account", "",
            Map.of(2025, new BigDecimal("100"), 2026, new BigDecimal("120")), true);

    @Test
    void usesTheCapForThatYear() {
        assertEquals(Optional.of(new BigDecimal("100")), CAPPED.depositCapFor(2025));
        assertEquals(Optional.of(new BigDecimal("120")), CAPPED.depositCapFor(2026));
    }

    @Test
    void laterYearsKeepTheLatestKnownCap() {
        assertEquals(Optional.of(new BigDecimal("120")), CAPPED.depositCapFor(2030));
    }

    @Test
    void yearsBeforeTheScheduleHaveNoCap() {
        assertEquals(Optional.empty(), CAPPED.depositCapFor(2024));
    }

    @Test
    void emptyScheduleMeansNoCap() {
        AccountProduct uncapped = new AccountProduct("open", "Open account", "", Map.of(), false);
        assertEquals(Optional.empty(), uncapped.depositCapFor(2026));
    }

    @Test
    void changingTheSourceMapLaterDoesNotChangeTheProduct() {
        Map<Integer, BigDecimal> caps = new HashMap<>(Map.of(2026, new BigDecimal("120")));
        AccountProduct product = new AccountProduct("capped", "Capped account", "", caps, true);
        caps.put(2026, new BigDecimal("999"));
        assertEquals(Optional.of(new BigDecimal("120")), product.depositCapFor(2026));
    }
}
