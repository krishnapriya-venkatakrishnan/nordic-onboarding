package dev.krishnapriya.onboarding.domain;

/**
 * A statement the customer must see, and if required, accept, before an account is opened.
 *
 * @param productId if set, the disclosure only applies when that product is chosen
 *                  (e.g. Denmark's one-per-person rule for Aktiesparekonto)
 */
public record Disclosure(String id, String text, boolean required, String productId) {

    public static Disclosure required(String id, String text) {
        return new Disclosure(id, text, true, null);
    }

    public static Disclosure optional(String id, String text) {
        return new Disclosure(id, text, false, null);
    }

    public static Disclosure requiredFor(String productId, String id, String text) {
        return new Disclosure(id, text, true, productId);
    }

    public boolean appliesTo(String chosenProductId) {
        return productId == null || productId.equals(chosenProductId);
    }
}
