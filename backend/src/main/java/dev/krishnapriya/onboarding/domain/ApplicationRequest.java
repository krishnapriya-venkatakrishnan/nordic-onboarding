package dev.krishnapriya.onboarding.domain;

import java.util.Set;

/** What the client submits at the end of the flow. */
public record ApplicationRequest(
        String identitySessionId,
        String nationalId,
        String fullName,
        String email,
        String productId,
        Set<String> acceptedDisclosures) {

    public ApplicationRequest {
        acceptedDisclosures = acceptedDisclosures == null ? Set.of() : Set.copyOf(acceptedDisclosures);
    }

    /** The generated record toString would print the ID number and contact details into logs and error traces. */
    @Override
    public String toString() {
        return "ApplicationRequest[productId=" + productId + ", acceptedDisclosures=" + acceptedDisclosures
                + ", personal details hidden]";
    }
}
