package dev.krishnapriya.onboarding.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Holds the markets that are built and the subset that is switched on.
 *
 * A market can be fully implemented and deployed but stay dark until its code is
 * added to the enabled set. That is how a new market ships to production early
 * and launches with a config change rather than a release.
 */
public final class MarketRegistry {

    private final Map<String, Market> enabled = new LinkedHashMap<>();

    public MarketRegistry(Collection<Market> available, Set<String> enabledCodes) {
        Set<String> wanted = normalize(enabledCodes);
        Set<String> seen = new HashSet<>();
        for (Market market : available) {
            String code = market.code().toUpperCase(Locale.ROOT);
            // Checked for every built market, not just enabled ones, so a clash fails the
            // deploy that ships the market dark instead of the config change that launches it.
            if (!seen.add(code)) {
                throw new IllegalStateException("Duplicate market code: " + code);
            }
            if (wanted.contains(code)) {
                enabled.put(code, market);
            }
        }
    }

    public Market get(String code) {
        Market market = code == null ? null : enabled.get(code.toUpperCase(Locale.ROOT));
        if (market == null) {
            throw new UnknownMarketException(code);
        }
        return market;
    }

    public List<Market> all() {
        return List.copyOf(enabled.values());
    }

    private static Set<String> normalize(Set<String> codes) {
        return codes.stream().map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }
}
