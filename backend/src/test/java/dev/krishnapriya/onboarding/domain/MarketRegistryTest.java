package dev.krishnapriya.onboarding.domain;

import dev.krishnapriya.onboarding.markets.dk.DenmarkMarket;
import dev.krishnapriya.onboarding.markets.se.SwedenMarket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketRegistryTest {

    private final List<Market> built = List.of(new SwedenMarket(), new DenmarkMarket());

    @Test
    void builtButDisabledMarketIsNotAvailable() {
        MarketRegistry registry = new MarketRegistry(built, Set.of("SE"));

        assertEquals(List.of("SE"), codes(registry));
        assertThrows(UnknownMarketException.class, () -> registry.get("DK"));
    }

    /** Listing a code before its market is built (or with a typo) launches nothing. */
    @Test
    void enabledCodeWithoutABuiltMarketIsIgnored() {
        MarketRegistry registry = new MarketRegistry(built, Set.of("SE", "NO"));

        assertEquals(List.of("SE"), codes(registry));
        assertThrows(UnknownMarketException.class, () -> registry.get("NO"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        MarketRegistry registry = new MarketRegistry(built, Set.of(" se ", "dk"));
        assertEquals("DK", registry.get("dk").code());
        assertEquals("SE", registry.get("Se").code());
    }

    /** The API lists markets in this order, so it must not depend on how the setting is written. */
    @Test
    void listsMarketsInTheOrderTheyWereBuilt() {
        MarketRegistry registry = new MarketRegistry(built, Set.of("DK", "SE"));
        assertEquals(List.of("SE", "DK"), codes(registry));
    }

    @Test
    void duplicateMarketCodesFailFast() {
        assertThrows(IllegalStateException.class,
                () -> new MarketRegistry(List.of(new SwedenMarket(), new SwedenMarket()), Set.of("SE")));
    }

    /** A clash must show up when the market ships dark, not on launch day when it is switched on. */
    @Test
    void duplicateMarketCodesFailFastEvenWhileDark() {
        assertThrows(IllegalStateException.class,
                () -> new MarketRegistry(List.of(new SwedenMarket(), new SwedenMarket(), new DenmarkMarket()),
                        Set.of("DK")));
    }

    private static List<String> codes(MarketRegistry registry) {
        return registry.all().stream().map(Market::code).toList();
    }
}
