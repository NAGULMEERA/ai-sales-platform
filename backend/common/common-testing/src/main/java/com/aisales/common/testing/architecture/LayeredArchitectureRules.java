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
        return new ClassFileImporter().importPackages(
                basePackage + ".api",
                basePackage + ".application",
                basePackage + ".domain",
                basePackage + ".infrastructure");
    }

    public static void check(JavaClasses classes) {
        ArchRule domainIsolation = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..api..", "..infrastructure..")
                .allowEmptyShould(true);

        ArchRule infrastructureIsolation = noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..api..")
                .allowEmptyShould(true);

        domainIsolation.check(classes);
        infrastructureIsolation.check(classes);
    }

    public static void checkPackage(String basePackage) {
        check(importLayerClasses(basePackage));
    }
}
