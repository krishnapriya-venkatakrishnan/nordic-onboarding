package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.OnboardingEvent;
import dev.krishnapriya.onboarding.domain.OnboardingEvent.Outcome;
import dev.krishnapriya.onboarding.domain.OnboardingService;
import dev.krishnapriya.onboarding.domain.RejectionReason;
import dev.krishnapriya.onboarding.domain.StepType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The format wired up the way the container does it (one Spring property), checked on the real
 * console output rather than on the formatter alone.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "logging.structured.format.console=dev.krishnapriya.onboarding.logging.CloudLoggingFormat")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    OnboardingService onboarding;

    @Test
    void onboardingEventsAreLoggedAsFieldsWithIdsMasked(CapturedOutput output) {
        new OnboardingLog().record(new OnboardingEvent("DK", StepType.REVIEW, Outcome.ACCEPTED, null));
        LoggerFactory.getLogger(StructuredLoggingTest.class)
                .warn("Rejected 121285-1234", new IllegalArgumentException("bad 010115-4123"));

        JsonNode accepted = lineFrom(output, "\"outcome\":\"ACCEPTED\"");
        assertEquals("DK", accepted.get("market").asString());
        assertEquals("REVIEW", accepted.get("step").asString());
        assertEquals("INFO", accepted.get("severity").asString());
        assertNull(accepted.get("reason"), "no reason field unless something was rejected");

        assertEquals("WARNING", lineFrom(output, "Rejected").get("severity").asString());
        assertFalse(output.getOut().contains("121285-1234"), "ID number in the log output");
        assertFalse(output.getOut().contains("010115-4123"), "ID number in a logged stack trace");
    }

    /** The real path: the service reports the event, the wiring sends it to the log as JSON. */
    @Test
    void aRejectedIdCheckIsLoggedWithItsReasonButNotTheNumber(CapturedOutput output) {
        onboarding.checkNationalId("dk", "010115-4123");

        JsonNode rejected = lineFrom(output, "\"outcome\":\"ID_REJECTED\"");
        assertEquals("DK", rejected.get("market").asString());
        assertEquals("PERSONAL_DETAILS", rejected.get("step").asString());
        assertEquals(RejectionReason.ID_UNDER_AGE.name(), rejected.get("reason").asString());
        assertFalse(output.getOut().contains("010115-4123"), "ID number in the log output");
    }

    private JsonNode lineFrom(CapturedOutput output, String containing) {
        String line = output.getOut().lines().filter(l -> l.contains(containing)).reduce((a, b) -> b).orElseThrow();
        return json.readTree(line);
    }
}
