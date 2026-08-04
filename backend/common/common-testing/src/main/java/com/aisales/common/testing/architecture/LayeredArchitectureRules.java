package com.aisales.common.testing.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Shared ArchUnit rules enforcing DDD layer boundaries within a single bounded context
 * (one microservice's own base package). Every service's {@code ArchitectureTest} delegates
 * to {@link #checkPackage(String)}, so extending the rules here applies them platform-wide
 * without touching each service's test class.
 *
 * <p>Rules enforced by {@link #checkPackage(String)} (see
 * {@code docs/03-architecture/architecture-governance.md} for full rationale):
 * <ol>
 *   <li>Domain must not depend on the service's own {@code api} package (controllers/DTOs).</li>
 *   <li>Domain must not depend on {@code infrastructure} (persistence, messaging, external clients).</li>
 *   <li>Domain must not depend on Spring MVC / Servlet types — domain stays framework-free.</li>
 *   <li>Infrastructure must not depend on the service's own {@code api} package.</li>
 *   <li>Controllers ({@code ..api.controller..}) must not depend on repositories
 *       ({@code ..infrastructure.persistence..}) directly — they must go through an
 *       Application Service.</li>
 * </ol>
 *
 * <p>{@link #packageSlicesMustBeFreeOfCycles(String)} is available as an <b>opt-in</b> check but
 * is intentionally <b>not</b> run by {@link #checkPackage(String)}. A coarse, top-level-package
 * slice (e.g. {@code domain} / {@code application} / {@code api} / {@code infrastructure})
 * routinely "cycles" in a healthy Spring Boot service for reasons that are not architecture
 * violations: application services legitimately depend on {@code infrastructure.persistence}
 * repositories, while {@code infrastructure.messaging} adapters (Kafka consumers) legitimately
 * call back into {@code application} services to invoke a use case — two valid dependency
 * directions between the same two slices. Repository-wide validation confirmed this pattern
 * is pervasive (ai, integration, lead, tenant, and workflow services), so enforcing it by
 * default would flag correct, working code rather than an accidental coupling. Platform-level
 * circular-dependency detection is instead performed at the Maven module graph level by the
 * {@code architecture-tests} module, which is the layer at which a cycle would actually be an
 * architecture defect (see the governance doc).
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

        domainIsolation(ownApiPackage).check(classes);
        infrastructureIsolation(ownApiPackage).check(classes);
        domainMustNotDependOnSpringMvc().check(classes);
        controllersMustNotAccessRepositoriesDirectly().check(classes);
    }

    /**
     * Domain must never depend on the service's own delivery layer (controllers/API DTOs)
     * or on infrastructure (persistence, messaging, external clients). Domain rules must be
     * testable and reusable independent of transport or storage technology.
     */
    public static ArchRule domainIsolation(String ownApiPackage) {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(ownApiPackage, "..infrastructure..")
                .allowEmptyShould(true)
                .as("Domain layer must not depend on the API or Infrastructure layers");
    }

    /**
     * Infrastructure (adapters: persistence, messaging, external clients) must not depend
     * back on the delivery layer. Infrastructure implements ports used by domain/application;
     * it never needs to know about controllers or request/response DTOs.
     */
    public static ArchRule infrastructureIsolation(String ownApiPackage) {
        return noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(ownApiPackage)
                .allowEmptyShould(true)
                .as("Infrastructure layer must not depend on the API layer");
    }

    /**
     * Domain must remain framework-free: no Spring MVC / Servlet coupling. Business rules and
     * invariants must be usable outside an HTTP request context (workflows, schedulers, tests).
     */
    public static ArchRule domainMustNotDependOnSpringMvc() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
                .allowEmptyShould(true)
                .as("Domain layer must not depend on Spring MVC or the Servlet API");
    }

    /**
     * Controllers must never reach into repositories directly. Every business operation flows
     * through an Application Service, which owns transaction boundaries, security/tenant
     * validation, and event publication (Rule 04 - REST API Governance).
     */
    public static ArchRule controllersMustNotAccessRepositoriesDirectly() {
        return noClasses()
                .that().resideInAPackage("..api.controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure.persistence..")
                .allowEmptyShould(true)
                .as("Controllers must not access repositories directly; go through an Application Service");
    }

    /**
     * Opt-in: detects circular dependencies between the top-level packages directly under
     * {@code basePackage}. Not part of {@link #checkPackage(String)} by default — see the
     * class-level Javadoc for why a coarse 4-layer slice check false-positives in this codebase.
     * Useful for a service that wants to enforce cycle-freedom between its own finer-grained,
     * intentionally independent modules (e.g. sibling bounded contexts that must not collaborate).
     */
    public static ArchRule packageSlicesMustBeFreeOfCycles(String basePackage) {
        return SlicesRuleDefinition.slices()
                .matching(basePackage + ".(*)..")
                .should().beFreeOfCycles();
    }

    public static void checkPackage(String basePackage) {
        check(importLayerClasses(basePackage), basePackage);
    }
}
