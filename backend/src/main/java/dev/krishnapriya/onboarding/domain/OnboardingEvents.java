package dev.krishnapriya.onboarding.domain;

/**
 * Where onboarding events go. The domain only reports them, so it stays free of any logging
 * framework; the logging package decides how they are written.
 */
public interface OnboardingEvents {

    void record(OnboardingEvent event);
}
