package dev.krishnapriya.onboarding.domain;

/** The national eID used to verify a person, e.g. BankID (SE) or MitID (DK). Simulated in this demo. */
public record IdentityProvider(String id, String displayName) {
}
