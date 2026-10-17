package dev.krishnapriya.onboarding.domain;

/** A problem with one field: a code for counting, a message for the person. */
public record FieldError(String field, RejectionReason code, String message) {
}
