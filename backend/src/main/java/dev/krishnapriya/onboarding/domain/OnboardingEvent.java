package dev.krishnapriya.onboarding.domain;

/**
 * Something that happened in an application, recorded so it can be counted per market.
 * Only codes, never personal data: there is simply no field to put an ID number in.
 *
 * @param reason why it was rejected; null for outcomes that aren't rejections
 */
public record OnboardingEvent(String market, StepType step, Outcome outcome, RejectionReason reason) {

    public enum Outcome {
        VERIFIED,
        ID_REJECTED,
        SUBMITTED,
        ACCEPTED,
        REJECTED
    }

    static OnboardingEvent of(Market market, StepType step, Outcome outcome) {
        return new OnboardingEvent(market.code(), step, outcome, null);
    }

    static OnboardingEvent rejected(Market market, StepType step, Outcome outcome, RejectionReason reason) {
        return new OnboardingEvent(market.code(), step, outcome, reason);
    }
}
