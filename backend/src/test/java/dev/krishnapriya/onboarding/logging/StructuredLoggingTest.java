package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.StepType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The format wired up the way the container does it (one Spring property), checked on the real
 * console output rather than on the formatter alone.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "logging.structured.format.console=dev.krishnapriya.onboarding.logging.CloudLoggingFormat")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void onboardingStepsAreLoggedAsFieldsWithIdsMasked(CapturedOutput output) {
        OnboardingLog.record("DK", StepType.PERSONAL_DETAILS, "ID_REJECTED");
        LoggerFactory.getLogger(StructuredLoggingTest.class)
                .warn("Rejected 121285-1234", new IllegalArgumentException("bad 010115-4123"));

        JsonNode step = lineFrom(output, "\"logger\":\"onboarding\"");
        assertEquals("DK", step.get("market").asString());
        assertEquals("PERSONAL_DETAILS", step.get("step").asString());
        assertEquals("ID_REJECTED", step.get("outcome").asString());
        assertEquals("INFO", step.get("severity").asString());

        assertEquals("WARNING", lineFrom(output, "Rejected").get("severity").asString());
        assertFalse(output.getOut().contains("121285-1234"), "ID number in the log output");
        assertFalse(output.getOut().contains("010115-4123"), "ID number in a logged stack trace");
    }

    private JsonNode lineFrom(CapturedOutput output, String containing) {
        String line = output.getOut().lines().filter(l -> l.contains(containing)).reduce((a, b) -> b).orElseThrow();
        return json.readTree(line);
    }
}
