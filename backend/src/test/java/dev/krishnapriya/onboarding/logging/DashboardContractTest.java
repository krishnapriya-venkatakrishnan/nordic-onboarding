package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.OnboardingEvent.Outcome;
import dev.krishnapriya.onboarding.domain.RejectionReason;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dashboard in infra/ asks the metric for labels and values by name. Renaming an outcome in
 * the code, or a typo in the dashboard, would leave a chart empty without any error; this fails
 * the build instead.
 */
class DashboardContractTest {

    private static final Path DASHBOARD = Path.of("../infra/dashboards/onboarding.json");
    private static final Path METRIC = Path.of("../infra/metrics/onboarding_events.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final String dashboard;
    private final JsonNode metric;

    DashboardContractTest() throws IOException {
        dashboard = Files.readString(DASHBOARD);
        metric = json.readTree(Files.readString(METRIC));
    }

    @Test
    void everyChartReadsTheOnboardingMetric() {
        Set<String> filters = new HashSet<>();
        json.readTree(dashboard).findValues("filter").forEach(f -> filters.add(f.asString()));

        assertFalse(filters.isEmpty());
        filters.forEach(f -> assertTrue(
                f.contains("metric.type=\"logging.googleapis.com/user/onboarding_events\""), f));
    }

    @Test
    void usesOnlyLabelsTheMetricHas() {
        Set<String> metricLabels = new HashSet<>();
        metric.get("metricDescriptor").get("labels").forEach(l -> metricLabels.add(l.get("key").asString()));

        Set<String> used = matches("metric\\.labels?\\.(\\w+)");
        assertFalse(used.isEmpty());
        assertTrue(metricLabels.containsAll(used), () -> "unknown labels: " + used);
    }

    @Test
    void filtersOnlyOnOutcomesTheCodeProduces() {
        Set<String> known = Arrays.stream(Outcome.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> used = quotedValuesAfter("metric\\.label\\.outcome=");

        assertTrue(known.containsAll(used), () -> "unknown outcomes: " + used);
        assertTrue(used.containsAll(Set.of("VERIFIED", "SUBMITTED", "ACCEPTED", "REJECTED", "ID_REJECTED")));
    }

    @Test
    void filtersOnlyOnReasonsTheCodeProduces() {
        Set<String> known = Arrays.stream(RejectionReason.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> used = quotedValuesAfter("metric\\.label\\.reason=");
        assertTrue(known.containsAll(used), () -> "unknown reasons: " + used);
    }

    @Test
    void showsSwedenAndDenmarkSideBySide() {
        assertEquals(Set.of("SE", "DK"), quotedValuesAfter("metric\\.label\\.market="));
    }

    private Set<String> matches(String regex) {
        Set<String> found = new HashSet<>();
        Matcher m = Pattern.compile(regex).matcher(dashboard);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    /** Values in `label="X"` or `label=one_of("X","Y")`, as written inside the JSON strings. */
    private Set<String> quotedValuesAfter(String label) {
        Set<String> found = new HashSet<>();
        Matcher m = Pattern.compile(label + "(?:one_of\\(([^)]*)\\)|\\\\\"(\\w+)\\\\\")").matcher(dashboard);
        while (m.find()) {
            if (m.group(2) != null) {
                found.add(m.group(2));
            } else {
                Matcher value = Pattern.compile("\\\\\"(\\w+)\\\\\"").matcher(m.group(1));
                while (value.find()) {
                    found.add(value.group(1));
                }
            }
        }
        return found;
    }
}
