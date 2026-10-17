package dev.krishnapriya.onboarding.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One JSON line per log event, in the shape Google Cloud Logging reads: it maps "severity" to the
 * log level and "message" to the summary line, and keeps every other field (market, step,
 * outcome, ...) as a searchable field that log-based metrics can count.
 *
 * Everything written, including stack traces, passes through {@link NationalIdMask}.
 * Switched on in the container by LOGGING_STRUCTURED_FORMAT_CONSOLE (see the Dockerfile).
 */
public class CloudLoggingFormat implements StructuredLogFormatter<ILoggingEvent> {

    private final JsonWriter<ILoggingEvent> json = JsonWriter.<ILoggingEvent>of(members -> {
        members.add("time", event -> event.getInstant().toString());
        members.add("severity", event -> severity(event.getLevel()));
        members.add("logger", ILoggingEvent::getLoggerName);
        members.add("message", CloudLoggingFormat::message);
        members.addMapEntries(CloudLoggingFormat::fields);
    }).withNewLineAtEnd();

    @Override
    public String format(ILoggingEvent event) {
        return json.writeToString(event);
    }

    /** Cloud Logging's level names; WARN is called WARNING there, and TRACE has no equivalent. */
    private static String severity(Level level) {
        return switch (level.toInt()) {
            case Level.ERROR_INT -> "ERROR";
            case Level.WARN_INT -> "WARNING";
            case Level.INFO_INT -> "INFO";
            default -> "DEBUG";
        };
    }

    /** The stack trace goes into the message, which is where Cloud Logging and Error Reporting look for it. */
    private static String message(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (event.getThrowableProxy() != null) {
            message = message + "\n" + ThrowableProxyUtil.asString(event.getThrowableProxy());
        }
        return NationalIdMask.scrub(message);
    }

    /** Fields from the SLF4J fluent API (addKeyValue) and the MDC, masked like the message. */
    private static Map<String, String> fields(ILoggingEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        event.getMDCPropertyMap().forEach((key, value) -> fields.put(key, NationalIdMask.scrub(value)));
        if (event.getKeyValuePairs() != null) {
            for (KeyValuePair pair : event.getKeyValuePairs()) {
                fields.put(pair.key, NationalIdMask.scrub(String.valueOf(pair.value)));
            }
        }
        return fields;
    }
}
