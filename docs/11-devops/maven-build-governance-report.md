# Maven Build Governance Report

**Status:** Completed
**Scope:** `backend/` reactor (41 Maven modules: common, infrastructure, services, plugins)
**Business code changed:** None. This was a build/dependency-hygiene pass only.

---

## 1. Objectives and Outcomes

| Objective | Outcome |
|---|---|
| Remove duplicate dependencies | No literal duplicate `<dependency>` declarations existed within any single POM (verified programmatically). Removed 18 **redundant** declarations of `archunit-junit5` that duplicated a dependency already available transitively. Added `maven-enforcer-plugin`'s `banDuplicatePomDependencyVersions` rule so any future duplicate fails the build. |
| Remove unused dependencies | Removed `com.aisales:common-core` and `com.aisales:common-contracts` from `common-testing` (unused in its own source), `org.projectlombok:lombok` from `common-testing` (unused, and already available to every consumer via `common-core`), and `archunit-junit5` from 18 service POMs (unused directly — only `common-testing`'s core ArchUnit usage is needed). |
| Detect dependency conflicts | Ran `dependency:tree -Dverbose` reactor-wide. Found two real convergence conflicts: `org.apache.httpcomponents:httpclient` (4.5.3 vs 4.5.13, via Eureka vs AWS SDK) and `org.jetbrains:annotations` (13.0 vs 17.0.0, via Kotlin stdlib vs Testcontainers). Both resolved by pinning in `dependencyManagement`. |
| Add Maven Enforcer | Added, bound to `validate` phase, reactor-wide, with 4 rules (see §3). |
| Add duplicate finder | Added `org.basepom.maven:duplicate-finder-maven-plugin`, bound to `verify` phase, report mode (see §3). Zero conflicting classes found reactor-wide. |
| Validate dependency convergence | Evaluated both `dependencyConvergence` (too strict — fails on deep Netflix Eureka-internal transitive graph unrelated to platform code) and `requireUpperBoundDependencies` (adopted — see §4 for the documented exception). |
| Ensure consistent dependency management | Centralized 5 previously ad-hoc, hardcoded dependency versions into root `dependencyManagement`; removed 7 redundant explicit `${project.version}` pins on `plugin-sdk` that duplicated the managed version. |

**Verification:** `mvnw clean install -DskipTests` succeeded across all 41 modules with the new Enforcer, Duplicate Finder and `dependency:analyze` executions active. No business/application code was modified.

---

## 2. Changes Made

### 2.1 Root `backend/pom.xml`

**New properties**

```xml
<maven-enforcer-plugin.version>3.6.3</maven-enforcer-plugin.version>
<duplicate-finder-maven-plugin.version>2.0.1</duplicate-finder-maven-plugin.version>
<maven-dependency-plugin.version>3.11.0</maven-dependency-plugin.version>

<httpclient.version>4.5.14</httpclient.version>
<jetbrains-annotations.version>17.0.0</jetbrains-annotations.version>
<logstash-logback-encoder.version>7.4</logstash-logback-encoder.version>
<pdfbox.version>3.0.3</pdfbox.version>
<aws-sdk-s3.version>2.25.70</aws-sdk-s3.version>
```

**`dependencyManagement` additions**

- `org.apache.httpcomponents:httpclient:4.5.14` — resolves the Eureka (4.5.3) vs AWS SDK Apache client (4.5.13) convergence conflict by pinning a single, newer version reactor-wide.
- `org.jetbrains:annotations:17.0.0` — resolves the Kotlin stdlib (13.0) vs Testcontainers `duct-tape` (17.0.0) conflict; compile-time-only annotations, safe to take the newer version explicitly.
- `net.logstash.logback:logstash-logback-encoder:7.4`, `org.apache.pdfbox:pdfbox:3.0.3`, `software.amazon.awssdk:s3:2.25.70` — centralizes 3 versions that were previously hardcoded ad-hoc inside `common-observability`, `ai-service` and `media-service` respectively.
- `com.tngtech.archunit:archunit:1.2.1` replaces the previous `archunit-junit5` managed entry (see §2.2/§2.3 — nothing in the reactor uses the JUnit 5 ArchUnit extension module; every architecture test goes through `common-testing`'s `LayeredArchitectureRules`, which only calls core ArchUnit classes).

**New plugin executions** (`build/plugins`, all reactor-wide via `pluginManagement` + root activation) — see §3 for full detail on each.

### 2.2 `common/common-testing/pom.xml`

- Removed `com.aisales:common-core` — unused; no class in `common-testing`'s source references it, and every consumer of `common-testing` already obtains `common-core` transitively via `common-starter` (main scope), independent of `common-testing`.
- Removed `com.aisales:common-contracts` — same reasoning, verified via source search.
- Removed `org.projectlombok:lombok` — unused; no Lombok annotations exist in `common-testing`'s source; every one of the 18 consuming services already declares Lombok directly for its own annotation processing.
- Replaced `com.tngtech.archunit:archunit-junit5` with `com.tngtech.archunit:archunit` (core only) — `common-testing`'s only ArchUnit usage (`LayeredArchitectureRules`) imports `ClassFileImporter`, `JavaClasses`, `ArchRule`, `SlicesRuleDefinition` — all core classes. The JUnit 5 integration module (`@AnalyzeClasses`, `@ArchTest`) is never used anywhere in the reactor; every service's `ArchitectureTest` is a plain `@Test` that calls `LayeredArchitectureRules.checkPackage(...)`.

### 2.3 18 service POMs — removed redundant `archunit-junit5` test dependency

`ai-service`, `analytics-service`, `appointment-service`, `audit-service`, `billing-service`, `catalog-service`, `conversation-service`, `customer-service`, `deal-service`, `identity-service`, `integration-service`, `lead-service`, `marketplace-service`, `media-service`, `notification-service`, `search-service`, `tenant-service`, `workflow-service`.

Each declared `com.tngtech.archunit:archunit-junit5` (test scope) directly, but:
1. No service source uses `com.tngtech.archunit.junit5.*` (`@AnalyzeClasses`/`@ArchTest`) — confirmed via full-text search.
2. Every service's `ArchitectureTest.java` only calls `LayeredArchitectureRules.checkPackage(...)` from `common-testing`, which every service already depends on (test scope).
3. ArchUnit core (now `archunit`, formerly transitively included by `archunit-junit5`) is available to every service's test classpath transitively through `common-testing`.

This is a duplicate declaration of a dependency already reachable transitively, and its removal has zero effect on compiled/test bytecode.

### 2.4 `plugins/plugin-sdk/pom.xml`

- Removed `com.aisales:common-core` — `plugin-sdk` does not use any `common-core` API; it was declared only to transitively pull in Jackson (`jackson-core`/`jackson-databind`), which `plugin-sdk` uses directly (`ObjectMapper`, `TypeReference` in `PipelineTemplateConfigs`).
- Added explicit `com.fasterxml.jackson.core:jackson-core` and `com.fasterxml.jackson.core:jackson-databind` (versions inherited from the Spring Boot BOM via the `plugins-parent` → root POM parent chain) — declares what `plugin-sdk` actually uses instead of relying on an unrelated common module as a version-transport mechanism.

### 2.5 7 POMs — removed redundant `${project.version}` on `plugin-sdk`

`marketplace-service`, `real-estate-plugin`, `automobile-plugin`, `natural-farming-plugin`, `whatsapp-channel-plugin`, `email-channel-plugin`, `meta-lead-ads-plugin`.

Each declared `<version>${project.version}</version>` on the `com.aisales:plugin-sdk` dependency, duplicating the version already fixed in root `dependencyManagement`. Removed; resolution is unchanged (both evaluate to the same reactor version).

### 2.6 3 POMs — moved hardcoded versions into `dependencyManagement`

- `common/common-observability/pom.xml` — `logstash-logback-encoder` version removed (now managed).
- `services/ai-service/pom.xml` — `pdfbox` version removed (now managed).
- `services/media-service/pom.xml` — `software.amazon.awssdk:s3` version removed (now managed).

---

## 3. New Build Governance Tooling

All three plugins below are reactor-wide (declared once in root `pom.xml`, apply to all 41 modules).

### 3.1 Maven Enforcer Plugin (`validate` phase, build-breaking)

```xml
<rules>
    <banDuplicatePomDependencyVersions/>
    <requireMavenVersion><version>[3.6.3,)</version></requireMavenVersion>
    <requireJavaVersion><version>[21,)</version></requireJavaVersion>
    <requireUpperBoundDeps>
        <excludes>
            <exclude>org.glassfish.hk2:hk2-api</exclude>
            <exclude>com.google.errorprone:error_prone_annotations</exclude>
        </excludes>
    </requireUpperBoundDeps>
</rules>
```

- **`banDuplicatePomDependencyVersions`** — directly satisfies "remove/detect duplicate dependencies" as an ongoing gate, not just a one-time cleanup.
- **`requireMavenVersion` / `requireJavaVersion`** — codifies the toolchain the project already assumes (`java.version=21`, current wrapper pins Maven 3.9.6) as an explicit, fail-fast check instead of an implicit assumption.
- **`requireUpperBoundDeps`** — the practical dependency-convergence gate (see §4 for why this was chosen over `dependencyConvergence`, and why two artifacts are excluded).

### 3.2 Duplicate Finder Maven Plugin (`verify` phase, report-only)

```xml
<configuration>
    <failBuildInCaseOfConflict>false</failBuildInCaseOfConflict>
    <failBuildInCaseOfDifferentContentConflict>false</failBuildInCaseOfDifferentContentConflict>
    ...
</configuration>
```

Detects duplicate classes/resources shaded onto the classpath by two different jars (classic causes: multiple SLF4J bindings, `javax.*` vs `jakarta.*` namespace clashes, shaded-vs-unshaded artifacts). **Result: zero conflicts found reactor-wide.** Left as report-only (non-failing) since it is a new regression guard rather than something actively fixing a current problem — flip `failBuildInCaseOfConflict` to `true` once a team has reviewed a full report on a clean run.

### 3.3 Maven Dependency Plugin `analyze-only` (`verify` phase, report-only)

Runs `dependency:analyze` on every module with an explicit ignore-list for the class of false positives inherent to this codebase's design (Spring Boot starter aggregators, Lombok/MapStruct annotation processors, JDBC/Flyway SPI-loaded drivers, and `common-testing`'s intentional test-dependency re-export role). Left non-failing (`failOnWarning=false`) because Spring Boot's starter-of-starters model produces structural "unused declared dependency" noise that does not indicate a real problem; use `target/dependency-analysis` output as a manual review aid when adding/removing dependencies, not as an automatic gate.

---

## 4. Design Decision: `requireUpperBoundDeps` over `dependencyConvergence`

Both rules were tested against the full reactor.

- **`dependencyConvergence`** (strict — every dependency must resolve to exactly one version across the whole tree) failed on `service-registry` with a large number of violations, nearly all originating **inside** `spring-cloud-starter-netflix-eureka-server`'s own transitive graph (Guava, Gson, Woodstox, HK2, Jersey) — none of which are referenced by platform code. Enforcing full convergence here would mean pinning versions of third-party internals the platform never calls, purely to satisfy the rule.
- **`requireUpperBoundDeps`** (the resolved version must be ≥ the highest version required anywhere in the tree — Maven's own "nearest wins" mediation must not accidentally pick a *lower* version) is the practical middle ground used by most large Spring Cloud codebases for exactly this reason. After pinning `httpclient` in `dependencyManagement` (§2.1), this rule flagged exactly two remaining violations, both confined to `service-registry` and both **inside** Eureka's own dependency graph:
  - `com.google.errorprone:error_prone_annotations` (2.23.0 vs 2.41.0, via `guava` vs `netflix-infix`→`gson`)
  - `org.glassfish.hk2:hk2-api` (3.1.1 vs 4.0.0-M3, via `spring-bridge` vs `jersey-hk2`)

  Forcing convergence on `hk2-api` specifically would require pinning across a **major version boundary into an unreleased milestone** (`4.0.0-M3`) of a dependency-injection framework used only inside Eureka Server's own Jersey internals — never touched by platform code. The risk of an unverified cross-major pin outweighs the benefit of silencing an enforcer warning about a third-party library's own internal inconsistency. **Both artifacts are excluded with inline documentation** in the enforcer rule (Rule 01 requires every architectural exception to be documented — see the comment directly above `<requireUpperBoundDeps>` in `backend/pom.xml`).

This is the only knowingly-accepted exception in the new governance setup; everything else the tooling checks is either clean or was fixed.

---

## 5. Remaining Recommendations (not applied — flagged for team decision)

1. **`duplicate-finder-maven-plugin` is currently report-only.** Once a team member reviews one full `mvn verify` run's report with fresh eyes, consider flipping `failBuildInCaseOfConflict` to `true` to make it a hard gate.
2. **`maven-dependency-plugin:analyze-only` is currently report-only (`failOnWarning=false`).** The ignore-list in root `pom.xml` covers the known structural false positives; if the team wants a hard "no unused/undeclared dependency" gate, this would need per-module `usedDependencies`/`ignoredUnusedDeclaredDependencies` tuning beyond what's practical to verify safely as an automated pass — recommend doing this incrementally, module by module, with a human reviewing each real finding.
3. **`spring-boot.version` (4.1.0) / `spring-cloud.version` (2025.1.2) / Testcontainers (1.19.8)** are all several releases ahead of/behind each other's typical release cadence pairing — not a build-correctness issue today (the reactor builds and the Enforcer passes), but worth a periodic BOM-alignment check as new Spring Boot/Cloud trains ship, since that is where the Eureka-internal HK2/error-prone drift (§4) originates.
4. **Consider a periodic `mvn versions:display-dependency-updates` / `versions:display-plugin-updates` job** (not added in this pass — no dependency version bumps were made beyond the 2 convergence fixes, per "introduce new dependencies/changes only when necessary") to track drift proactively rather than discovering it via Enforcer failures.

---

## 6. Files Modified

**Root build configuration**
- `backend/pom.xml`

**Common modules**
- `backend/common/common-testing/pom.xml`
- `backend/common/common-observability/pom.xml`

**Plugin SDK**
- `backend/plugins/plugin-sdk/pom.xml`

**18 service POMs** (removed redundant `archunit-junit5`)
- `backend/services/{ai,analytics,appointment,audit,billing,catalog,conversation,customer,deal,identity,integration,lead,marketplace,media,notification,search,tenant,workflow}-service/pom.xml`

**7 POMs** (removed redundant `${project.version}` on `plugin-sdk`)
- `backend/services/marketplace-service/pom.xml`
- `backend/plugins/industry/{real-estate,automobile,natural-farming}-plugin/pom.xml`
- `backend/plugins/capability/{whatsapp-channel,email-channel,meta-lead-ads}-plugin/pom.xml`

**2 POMs** (moved hardcoded version into `dependencyManagement`, in addition to `common-observability` above)
- `backend/services/ai-service/pom.xml` (pdfbox)
- `backend/services/media-service/pom.xml` (aws-sdk s3)

No `src/main/java` or `src/test/java` business/application code was modified anywhere in this pass.

---

## 7. Verification Performed

- `mvn validate --fail-at-end` reactor-wide — all 41 modules pass the new Enforcer rules.
- `mvn clean install -DskipTests --fail-at-end` reactor-wide — full compile + package + `duplicate-finder:check` + `dependency:analyze-only` succeeded for every module (one transient Windows file-handle error on `notification-service`'s `dependency:analyze-only` step self-resolved on retry/resume and did not recur — not related to any POM change).
- A custom POM-inventory script confirmed, post-change: zero duplicate `<dependency>` declarations within any single POM, zero child-POM explicit versions duplicating a root-managed version, and zero artifacts with inconsistent explicit versions across different POMs.
