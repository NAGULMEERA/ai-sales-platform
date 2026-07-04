package com.aisales.common.testing.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates core DDD layer boundaries within a bounded context.
 * External platform libraries (common-*, Spring) are allowed at application/infrastructure boundaries.
 */
public final class LayeredArchitectureRules {

    private LayeredArchitectureRules() {
    }

    public static JavaClasses importLayerClasses(String basePackage) {
        return new ClassFileImporter().importPackages(basePackage);
    }

    public static void check(JavaClasses classes) {
        check(classes, "..");
    }

    /**
     * @param basePackage the bounded context's own root package (e.g. {@code com.aisales.identity}),
     *                     used to scope the "api" package matcher precisely. A generic {@code "..api.."}
     *                     glob would also match third-party packages that merely contain an "api" segment
     *                     (e.g. {@code org.assertj.core.api}, {@code org.flywaydb.core.api}), producing
     *                     false-positive violations whenever test code under {@code ..infrastructure..}
     *                     calls into those libraries.
     */
    public static void check(JavaClasses classes, String basePackage) {
        String ownApiPackage = basePackage.equals("..") ? "..api.." : basePackage + ".api..";

        ArchRule domainIsolation = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(ownApiPackage, "..infrastructure..")
                .allowEmptyShould(true);

        ArchRule infrastructureIsolation = noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(ownApiPackage)
                .allowEmptyShould(true);

        domainIsolation.check(classes);
        infrastructureIsolation.check(classes);
    }

    public static void checkPackage(String basePackage) {
        check(importLayerClasses(basePackage), basePackage);
    }
}
