package dev.krishnapriya.onboarding.domain;

/**
 * Stable codes for why something was rejected. Messages are for people and may change wording;
 * these are for counting, so dashboards and alerts keep working when the text changes.
 */
public enum RejectionReason {
    IDENTITY_NOT_VERIFIED,
    ID_FORMAT,
    ID_CHECK_DIGIT,
    ID_INVALID_DATE,
    ID_FUTURE_DATE,
    ID_UNDER_AGE,
    NAME_MISSING,
    EMAIL_INVALID,
    PRODUCT_NOT_OFFERED,
    CONFIRMATION_MISSING
}
