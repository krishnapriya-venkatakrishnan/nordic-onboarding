package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.OnboardingEvent;
import dev.krishnapriya.onboarding.domain.OnboardingEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * Writes onboarding events as structured log fields (market, step, outcome, reason), so Cloud
 * Logging can count them. The events hold only codes, so nothing personal can end up here.
 */
public final class OnboardingLog implements OnboardingEvents {

    private static final Logger log = LoggerFactory.getLogger("onboarding");

    @Override
    public void record(OnboardingEvent event) {
        LoggingEventBuilder entry = log.atInfo()
                .addKeyValue("market", event.market())
                .addKeyValue("step", event.step().name())
                .addKeyValue("outcome", event.outcome().name());
        if (event.reason() != null) {
            entry = entry.addKeyValue("reason", event.reason().name());
        }
        entry.log("{} {} {}{}", event.market(), event.step(), event.outcome(),
                event.reason() == null ? "" : " " + event.reason());
    }
}
