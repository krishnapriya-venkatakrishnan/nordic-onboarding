package dev.krishnapriya.onboarding.logging;

import dev.krishnapriya.onboarding.domain.OnboardingEvent.Outcome;
import dev.krishnapriya.onboarding.domain.RejectionReason;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dashboard and alert policies in infra/ ask the metric for labels and values by name.
 * Renaming an outcome in the code, or a typo in a file, would leave a chart empty or an alert
 * silent without any error; this fails the build instead.
 */
class MonitoringConfigContractTest {

    private static final Path INFRA = Path.of("../infra");
    private static final String METRIC_TYPE = "metric.type=\"logging.googleapis.com/user/onboarding_events\"";

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode metric = read(INFRA.resolve("metrics/onboarding_events.json"));
    private final List<Path> files = filesIn("dashboards", "alerts");

    @Test
    void coversTheDashboardAndBothAlerts() {
        assertEquals(Set.of("onboarding.json", "service-down.json", "denmark-rejection-spike.json"),
                files.stream().map(f -> f.getFileName().toString()).collect(Collectors.toSet()));
    }

    @Test
    void usesOnlyLabelsTheMetricHas() {
        Set<String> metricLabels = new HashSet<>();
        metric.get("metricDescriptor").get("labels").forEach(l -> metricLabels.add(l.get("key").asString()));
        for (Path file : files) {
            String text = onboardingFilters(file);
            Set<String> used = matches(text, "metric\\.labels?\\.(\\w+)");
            assertTrue(metricLabels.containsAll(used), () -> file + " uses unknown labels: " + used);
        }
    }

    @Test
    void filtersOnlyOnOutcomesReasonsAndMarketsThatExist() {
        Set<String> outcomes = Arrays.stream(Outcome.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> reasons = Arrays.stream(RejectionReason.values()).map(Enum::name).collect(Collectors.toSet());
        for (Path file : files) {
            String text = onboardingFilters(file);
            Set<String> o = valuesOf(text, "outcome");
            Set<String> r = valuesOf(text, "reason");
            Set<String> m = valuesOf(text, "market");
            assertTrue(outcomes.containsAll(o), () -> file + " uses unknown outcomes: " + o);
            assertTrue(reasons.containsAll(r), () -> file + " uses unknown reasons: " + r);
            assertTrue(Set.of("SE", "DK").containsAll(m), () -> file + " uses unknown markets: " + m);
        }
    }

    /** The hourly synthetic sign-up must not show up as customers, nor hide a Danish spike. */
    @Test
    void everyChartAndAlertLeavesOutSyntheticTraffic() {
        for (Path file : files) {
            JsonNode root = read(file);
            Stream.concat(root.findValues("filter").stream(), root.findValues("denominatorFilter").stream())
                    .map(JsonNode::asString)
                    .filter(f -> f.contains(METRIC_TYPE))
                    .forEach(f -> assertTrue(f.contains("metric.label.synthetic!=\"true\""), () -> file + ": " + f));
        }
    }

    @Test
    void dashboardShowsSwedenAndDenmarkSideBySide() {
        String text = onboardingFilters(INFRA.resolve("dashboards/onboarding.json"));
        assertEquals(Set.of("SE", "DK"), valuesOf(text, "market"));
        assertTrue(valuesOf(text, "outcome").containsAll(
                Set.of("VERIFIED", "SUBMITTED", "ACCEPTED", "REJECTED", "ID_REJECTED")));
    }

    /** The spike alert compares Denmark's REJECTED with its SUBMITTED; both must be real outcomes. */
    @Test
    void denmarkAlertComparesRejectedWithSubmitted() {
        JsonNode ratio = read(INFRA.resolve("alerts/denmark-rejection-spike.json"))
                .get("conditions").get(0).get("conditionThreshold");
        assertTrue(ratio.get("filter").asString().contains("metric.label.outcome=\"REJECTED\""));
        assertTrue(ratio.get("denominatorFilter").asString().contains("metric.label.outcome=\"SUBMITTED\""));
        assertTrue(ratio.get("filter").asString().contains("metric.label.market=\"DK\""));
    }

    /** All filter strings in a file that read the onboarding metric, joined. */
    private String onboardingFilters(Path file) {
        JsonNode root = read(file);
        Stream<JsonNode> filters = Stream.concat(
                root.findValues("filter").stream(), root.findValues("denominatorFilter").stream());
        String text = filters.map(JsonNode::asString).filter(f -> f.contains(METRIC_TYPE))
                .collect(Collectors.joining("\n"));
        // Templates and group-bys name labels too.
        text += "\n" + root.findValues("groupByFields").toString() + root.findValues("legendTemplate")
                + root.findValues("tableTemplate");
        return text;
    }

    /** Values in label="X" or label=one_of("X","Y"). */
    private static Set<String> valuesOf(String text, String label) {
        Set<String> found = new HashSet<>();
        Matcher m = Pattern.compile("metric\\.label\\." + label + "=(?:one_of\\(([^)]*)\\)|\"(\\w+)\")").matcher(text);
        while (m.find()) {
            if (m.group(2) != null) {
                found.add(m.group(2));
            } else {
                found.addAll(matches(m.group(1), "\"(\\w+)\""));
            }
        }
        return found;
    }

    private static Set<String> matches(String text, String regex) {
        Set<String> found = new HashSet<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    private List<Path> filesIn(String... folders) {
        return Arrays.stream(folders).flatMap(folder -> {
            try (Stream<Path> listed = Files.list(INFRA.resolve(folder))) {
                return listed.filter(p -> p.toString().endsWith(".json")).toList().stream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    private JsonNode read(Path file) {
        try {
            return json.readTree(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
