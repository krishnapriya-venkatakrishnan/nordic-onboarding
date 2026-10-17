package dev.krishnapriya.onboarding.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.krishnapriya.onboarding.domain.OnboardingEvent;
import dev.krishnapriya.onboarding.domain.OnboardingEvent.Outcome;
import dev.krishnapriya.onboarding.domain.RejectionReason;
import dev.krishnapriya.onboarding.domain.StepType;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The log-based metric in infra/ finds and counts log fields by name. This keeps that file and
 * the code in step: renaming a field here, or a typo there, fails the build instead of quietly
 * leaving a chart empty.
 */
class LogMetricContractTest {

    private static final Path METRIC = Path.of("../infra/metrics/onboarding_events.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode metric;

    LogMetricContractTest() throws IOException {
        metric = json.readTree(Files.readString(METRIC));
    }

    @Test
    void countsOnlyOnboardingEventsFromThisService() {
        String filter = metric.get("filter").asString();
        assertTrue(filter.contains("resource.labels.service_name=\"nordic-onboarding\""), filter);
        assertTrue(filter.contains("jsonPayload.logger=\"onboarding\""), filter);
    }

    @Test
    void everyLabelComesFromAFieldTheAppActuallyWrites() {
        JsonNode written = json.readTree(logLine(new OnboardingEvent(
                "DK", StepType.PERSONAL_DETAILS, Outcome.ID_REJECTED, RejectionReason.ID_UNDER_AGE)));
        assertEquals("onboarding", written.get("logger").asString(), "the filter's logger");

        Set<String> labels = new HashSet<>();
        for (JsonNode label : metric.get("metricDescriptor").get("labels")) {
            String key = label.get("key").asString();
            labels.add(key);
            String extractor = metric.get("labelExtractors").get(key).asString();
            assertTrue(extractor.matches("EXTRACT\\(jsonPayload\\.\\w+\\)"), extractor);
            String field = extractor.substring("EXTRACT(jsonPayload.".length(), extractor.length() - 1);
            assertTrue(written.has(field), () -> "the app writes no '" + field + "' field: " + written);
        }
        assertEquals(Set.of("market", "step", "outcome", "reason", "synthetic"), labels);
    }

    /**
     * Logs the event through the real OnboardingLog and formats it as the container would, as part
     * of a synthetic request, so every field a label can come from is present.
     */
    private String logLine(OnboardingEvent event) {
        Logger logger = (Logger) LoggerFactory.getLogger("onboarding");
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        logger.addAppender(captured);
        MDC.put("synthetic", "true");
        try {
            new OnboardingLog().record(event);
        } finally {
            MDC.remove("synthetic");
            logger.detachAppender(captured);
        }
        return new CloudLoggingFormat().format(captured.list.getFirst());
    }
}
