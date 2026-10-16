package dev.krishnapriya.onboarding.domain;

import java.util.List;

/** Either an opened account reference or the list of problems to fix. */
public record ApplicationResult(boolean accepted, String applicationId, String marketCode,
                                String productId, String currency, List<FieldError> errors) {

    public static ApplicationResult accepted(String id, String market, String product, String currency) {
        return new ApplicationResult(true, id, market, product, currency, List.of());
    }

    public static ApplicationResult rejected(String market, List<FieldError> errors) {
        return new ApplicationResult(false, null, market, null, null, List.copyOf(errors));
    }
}
