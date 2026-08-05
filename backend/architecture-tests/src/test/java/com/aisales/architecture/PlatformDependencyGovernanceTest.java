package com.aisales.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Platform-wide architecture governance: validates the Maven module dependency graph across
 * {@code common/}, {@code services/}, {@code plugins/} and {@code infrastructure/}.
 *
 * <p>These rules operate at a granularity ArchUnit cannot see from inside a single service's
 * test classpath (a service module never has another service's classes on its classpath in the
 * first place). Here we instead read every {@code pom.xml} in the reactor directly and assert on
 * the declared module dependency graph. See
 * {@code docs/03-architecture/architecture-governance.md} for the full rule catalogue and
 * rationale.
 *
 * <p>Rules enforced:
 * <ol>
 *   <li>{@link #commonModulesMustNeverDependOnServiceModules()} - Rule 01 dependency direction.</li>
 *   <li>{@link #servicesMustNotDependOnOtherServiceModulesDirectly()} - services never get another
 *       service's packages on their classpath; cross-service collaboration goes through REST,
 *       Kafka events, or an SDK/contracts module instead.</li>
 *   <li>{@link #pluginsMustDependOnlyOnPublishedContracts()} - industry and capability plugins may
 *       depend only on {@code plugin-sdk} (and the platform contracts it re-exports), never on a
 *       service or on Platform Core internals.</li>
 *   <li>{@link #moduleGraphMustBeFreeOfCircularDependencies()} - the whole reactor's module graph
 *       must be a DAG.</li>
 * </ol>
 */
class PlatformDependencyGovernanceTest {

    private static final String GROUP_ID = "com.aisales";

    /** Internal artifacts a plugin may compile against, in addition to its own {@code plugin-sdk}. */
    private static final Set<String> PLUGIN_ALLOWED_INTERNAL_DEPENDENCIES = Set.of("plugin-sdk", "common-core");

    private static Map<String, PomGraphLoader.MavenModule> modules;

    @BeforeAll
    static void loadModuleGraph() {
        Path repoRoot = findRepositoryRoot();
        modules = PomGraphLoader.loadModules(repoRoot.resolve("backend"), repoRoot.resolve("infrastructure"));

        assertThat(modules)
                .as("Maven module graph scan should discover the platform's modules under backend/ and infrastructure/")
                .isNotEmpty();
        assertThat(modules)
                .as("Sanity check: known modules must be discoverable")
                .containsKeys("common-core", "identity-service", "plugin-sdk", "real-estate-plugin");
    }

    /** Walks upward from the current working directory (the module's own base dir under Maven/Surefire) to find the git repository root. */
    private static Path findRepositoryRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve(".git"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Unable to locate the repository root (no .git directory found above " + Path.of("").toAbsolutePath() + ")");
    }

    private static boolean isServiceModule(String artifactId) {
        return artifactId.endsWith("-service") && modules.containsKey(artifactId);
    }

    private static boolean isCommonModule(String artifactId) {
        return artifactId.startsWith("common-");
    }

    private static boolean isPluginModule(PomGraphLoader.MavenModule module) {
        String normalizedPath = module.pomPath().toString().replace('\\', '/');
        return normalizedPath.contains("/plugins/") && !module.artifactId().equals("plugins-parent");
    }

    @Test
    @DisplayName("Common modules must never depend on service modules")
    void commonModulesMustNeverDependOnServiceModules() {
        List<String> violations = new ArrayList<>();
        for (PomGraphLoader.MavenModule module : modules.values()) {
            if (!isCommonModule(module.artifactId())) {
                continue;
            }
            for (String dependencyArtifactId : module.internalDependencyArtifactIds(GROUP_ID)) {
                if (isServiceModule(dependencyArtifactId)) {
                    violations.add(module.artifactId() + " -> " + dependencyArtifactId);
                }
            }
        }

        assertThat(violations)
                .as("Common modules are shared platform building blocks; a common -> service dependency "
                        + "would invert the allowed dependency direction (Rule 01 Part 2/4) and make the "
                        + "common module impossible to reuse or release independently. Violations: %s",
                        violations)
                .isEmpty();
    }

    @Test
    @DisplayName("Services must not depend on another service's module directly")
    void servicesMustNotDependOnOtherServiceModulesDirectly() {
        List<String> violations = new ArrayList<>();
        for (PomGraphLoader.MavenModule module : modules.values()) {
            if (!isServiceModule(module.artifactId())) {
                continue;
            }
            for (String dependencyArtifactId : module.internalDependencyArtifactIds(GROUP_ID)) {
                if (isServiceModule(dependencyArtifactId) && !dependencyArtifactId.equals(module.artifactId())) {
                    violations.add(module.artifactId() + " -> " + dependencyArtifactId);
                }
            }
        }

        assertThat(violations)
                .as("A Maven dependency from one service onto another would put the other service's internal "
                        + "packages on its classpath, bypassing REST/events/SDK boundaries entirely (Rule 01 "
                        + "Part 2: 'Service -> Another Service Implementation' is forbidden). Violations: %s",
                        violations)
                .isEmpty();
    }

    @Test
    @DisplayName("Plugins must depend only on published contracts (plugin-sdk)")
    void pluginsMustDependOnlyOnPublishedContracts() {
        List<String> violations = new ArrayList<>();
        for (PomGraphLoader.MavenModule module : modules.values()) {
            if (!isPluginModule(module) || module.artifactId().equals("plugin-sdk")) {
                continue;
            }
            for (String dependencyArtifactId : module.internalDependencyArtifactIds(GROUP_ID)) {
                if (!PLUGIN_ALLOWED_INTERNAL_DEPENDENCIES.contains(dependencyArtifactId)) {
                    violations.add(module.artifactId() + " -> " + dependencyArtifactId);
                }
            }
        }

        assertThat(violations)
                .as("Industry and Capability plugins must depend only on the published plugin-sdk contract "
                        + "(Rule 01 'Plugin -> Platform Internals' is forbidden). A plugin depending on a "
                        + "service or on other Platform Core internals would break plugin replaceability and "
                        + "industry-agnostic Platform Core. Violations: %s",
                        violations)
                .isEmpty();
    }

    @Test
    @DisplayName("The Maven module dependency graph must be free of circular dependencies")
    void moduleGraphMustBeFreeOfCircularDependencies() {
        Map<String, List<String>> graph = new HashMap<>();
        for (PomGraphLoader.MavenModule module : modules.values()) {
            List<String> internalDependencies = module.internalDependencyArtifactIds(GROUP_ID).stream()
                    .filter(modules::containsKey)
                    .collect(Collectors.toList());
            graph.put(module.artifactId(), internalDependencies);
        }

        List<String> cycle = CycleDetector.findCycle(graph);

        assertThat(cycle)
                .as("The platform's Maven reactor must form a directed acyclic graph. A cycle here means two "
                        + "or more modules cannot be built or versioned independently. Cycle found: %s", cycle)
                .isEmpty();
    }
}
