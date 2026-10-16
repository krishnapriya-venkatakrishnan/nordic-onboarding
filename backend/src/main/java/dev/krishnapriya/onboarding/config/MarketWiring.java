package dev.krishnapriya.onboarding.config;

import dev.krishnapriya.onboarding.domain.Market;
import dev.krishnapriya.onboarding.domain.MarketRegistry;
import dev.krishnapriya.onboarding.domain.OnboardingService;
import dev.krishnapriya.onboarding.markets.dk.DenmarkMarket;
import dev.krishnapriya.onboarding.markets.se.SwedenMarket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;
import java.util.Set;

/**
 * The only place Spring meets the domain. The domain and market packages have no
 * framework imports, so they are tested as plain Java.
 *
 * To add a market: register its bean here, then add its code to markets.enabled.
 */
@Configuration
public class MarketWiring {

    @Bean
    Market sweden() {
        return new SwedenMarket();
    }

    @Bean
    Market denmark() {
        return new DenmarkMarket();
    }

    @Bean
    MarketRegistry marketRegistry(List<Market> markets, @Value("${markets.enabled}") Set<String> enabled) {
        return new MarketRegistry(markets, enabled);
    }

    /** UTC on purpose: each market turns it into its own local date, so the server's zone never matters. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OnboardingService onboardingService(MarketRegistry registry, Clock clock) {
        return new OnboardingService(registry, clock);
    }
}
