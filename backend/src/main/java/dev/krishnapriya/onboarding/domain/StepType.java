package dev.krishnapriya.onboarding.domain;

/** Building blocks of the onboarding flow. The UI renders whichever steps a market lists, in that order. */
public enum StepType {
    IDENTITY,
    PERSONAL_DETAILS,
    ACCOUNT_TYPE,
    DISCLOSURES,
    REVIEW
}
