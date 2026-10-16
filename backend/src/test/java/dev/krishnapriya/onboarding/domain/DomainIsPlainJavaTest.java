package dev.krishnapriya.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Architecture rule 1: the domain is plain Java. It may only import the JDK and itself,
 * so no framework (and no market) can leak in. Checked on source so it needs no extra library.
 */
class DomainIsPlainJavaTest {

    static final Path DOMAIN = Path.of("src/main/java/dev/krishnapriya/onboarding/domain");

    @Test
    void domainImportsOnlyTheJdkAndItself() throws IOException {
        List<String> offending;
        try (Stream<Path> files = Files.walk(DOMAIN)) {
            offending = files.filter(f -> f.toString().endsWith(".java"))
                    .flatMap(DomainIsPlainJavaTest::importsOf)
                    .filter(i -> !i.startsWith("java.") && !i.startsWith("dev.krishnapriya.onboarding.domain."))
                    .toList();
        }
        assertEquals(List.of(), offending);
    }

    @Test
    void theCheckActuallyFindsDomainSources() throws IOException {
        try (Stream<Path> files = Files.walk(DOMAIN)) {
            assertFalse(files.noneMatch(f -> f.toString().endsWith("Market.java")));
        }
    }

    private static Stream<String> importsOf(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line.replaceFirst("^import\\s+(static\\s+)?", "").replace(";", "").strip()
                            + "  (" + file.getFileName() + ")");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
