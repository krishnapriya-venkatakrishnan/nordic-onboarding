package dev.krishnapriya.onboarding;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** The architecture rules from CLAUDE.md that can be checked mechanically, against production classes only. */
@AnalyzeClasses(packages = "dev.krishnapriya.onboarding", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Rule 1: the domain is plain Java. This also keeps it from knowing about any one market. */
    @ArchTest
    static final ArchRule domainIsPlainJava = classes()
            .that().resideInAPackage("dev.krishnapriya.onboarding.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "dev.krishnapriya.onboarding.domain..");

    /** Rule 2: markets never use each other, so a change for one country cannot break another. */
    @ArchTest
    static final ArchRule marketsAreIndependent = slices()
            .matching("dev.krishnapriya.onboarding.markets.(*)..")
            .should().notDependOnEachOther();
}
