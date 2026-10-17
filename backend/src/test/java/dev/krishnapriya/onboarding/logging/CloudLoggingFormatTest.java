package dev.krishnapriya.onboarding.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudLoggingFormatTest {

    private final CloudLoggingFormat format = new CloudLoggingFormat();
    private final LoggerContext logback = new LoggerContext();
    private final JsonMapper json = JsonMapper.builder().build();

    CloudLoggingFormatTest() {
        // A bare context has no MDC store; normal start-up gives it one.
        logback.setMDCAdapter(new LogbackMDCAdapter());
    }

    private String write(Level level, String message, Throwable thrown, String... keyValues) {
        LoggingEvent event = new LoggingEvent("test", logback.getLogger("onboarding"), level, message, thrown, null);
        for (int i = 0; i < keyValues.length; i += 2) {
            event.addKeyValuePair(new KeyValuePair(keyValues[i], keyValues[i + 1]));
        }
        return format.format(event);
    }

    @Test
    void writesOneJsonLineInCloudLoggingsShape() {
        String line = write(Level.INFO, "DK PERSONAL_DETAILS ID_REJECTED", null,
                "market", "DK", "step", "PERSONAL_DETAILS", "outcome", "ID_REJECTED");

        assertTrue(line.endsWith("\n"));
        assertEquals(1, line.strip().lines().count(), "one event, one line");
        JsonNode log = json.readTree(line);
        assertEquals("INFO", log.get("severity").asString());
        assertEquals("DK PERSONAL_DETAILS ID_REJECTED", log.get("message").asString());
        assertEquals("onboarding", log.get("logger").asString());
        assertDoesNotThrow(() -> Instant.parse(log.get("time").asString()));
        assertEquals("DK", log.get("market").asString());
        assertEquals("PERSONAL_DETAILS", log.get("step").asString());
        assertEquals("ID_REJECTED", log.get("outcome").asString());
    }

    @Test
    void usesCloudLoggingsLevelNames() {
        assertEquals("ERROR", json.readTree(write(Level.ERROR, "x", null)).get("severity").asString());
        assertEquals("WARNING", json.readTree(write(Level.WARN, "x", null)).get("severity").asString());
        assertEquals("DEBUG", json.readTree(write(Level.TRACE, "x", null)).get("severity").asString());
    }

    /** Rule 7: an ID number reaches neither the message, nor a field, nor a stack trace. */
    @Test
    void masksNationalIdsEverywhere() {
        String line = write(Level.ERROR, "Checked 19121212-1212",
                new IllegalStateException("Could not parse 010115-4123"), "input", "121285-1234");

        for (String id : new String[] {"19121212-1212", "010115-4123", "121285-1234"}) {
            assertFalse(line.contains(id), () -> "leaked " + id + " in " + line);
        }
        JsonNode log = json.readTree(line);
        assertTrue(log.get("message").asString().startsWith("Checked [national ID masked]"));
        assertTrue(log.get("message").asString().contains("IllegalStateException: Could not parse [national ID masked]"),
                "the stack trace is kept, with the ID masked");
        assertEquals("[national ID masked]", log.get("input").asString());
    }
}
