package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs one step of an application as structured fields, so Cloud Logging can count outcomes by
 * market and step. Never pass personal data here: the fields are meant to be counted.
 */
public final class OnboardingLog {

    private static final Logger log = LoggerFactory.getLogger("onboarding");

    private OnboardingLog() {
    }

    public static void record(String market, StepType step, String outcome) {
        log.atInfo()
                .addKeyValue("market", market)
                .addKeyValue("step", step.name())
                .addKeyValue("outcome", outcome)
                .log("{} {} {}", market, step, outcome);
    }
}
