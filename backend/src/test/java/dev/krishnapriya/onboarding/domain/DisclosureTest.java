package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisclosureTest {

    @Test
    void generalDisclosuresApplyWhateverProductIsChosen() {
        Disclosure terms = Disclosure.required("terms", "I accept the terms.");
        assertTrue(terms.required());
        assertTrue(terms.appliesTo("any-product"));
        assertTrue(terms.appliesTo(null));
    }

    @Test
    void productScopedDisclosuresApplyOnlyToThatProduct() {
        Disclosure scoped = Disclosure.requiredFor("capped", "one-only", "I hold no other account of this type.");
        assertTrue(scoped.required());
        assertTrue(scoped.appliesTo("capped"));
        assertFalse(scoped.appliesTo("open"));
        assertFalse(scoped.appliesTo(null));
    }

    @Test
    void optionalDisclosuresAreNotRequired() {
        assertFalse(Disclosure.optional("news", "Send me news.").required());
    }
}
