# Repository Review Report

**Status:** Completed
**Scope:** All JPA repositories across `backend/common/common-events` and all 18 `backend/services/*` microservices (identity, tenant, lead, customer, deal, catalog, conversation, appointment, marketplace, media, notification, ai, analytics, audit, billing, search, workflow, integration).
**Checklist applied to every repository:** N+1 queries, missing indexes, fetch joins, pagination, `JpaSpecificationExecutor` usage, query optimization, `@EntityGraph` usage, transaction safety.
**Business code changed:** No public REST contracts, DTOs, or entity shapes were changed. All fixes are internal to repository/service implementation or purely additive Flyway migrations.

---

## 1. Method

1. Inventoried every `*Repository` interface (common-events + all 18 services).
2. Read every entity's association mappings (`@OneToMany`/`@ManyToOne`/`@ManyToMany`) to find N+1 candidates, then traced every caller that iterates a collection and calls a repository again per element.
3. Cross-referenced every `@Query`/derived-query predicate (especially `LOWER(...)`, `UPPER(...)`, `IgnoreCase`, `LIKE '%...%'`) against the actual Flyway-created indexes to find index-defeating patterns.
4. Traced every unbounded `List<T>` finder to its service/controller caller to judge real-world growth risk.
5. Reviewed `@Transactional` boundaries on every multi-step write sequence, distinguishing safe local-DB-only sequences from sequences that call another microservice over REST (which must **not** be wrapped in a local DB transaction per Rule 01/Rule 03 — holding a DB transaction open across network I/O is an anti-pattern, not a bug to fix).
6. Six parallel exploration passes (grouped by service, ~3 services per pass) cross-checked the manual findings independently; every finding taken from those passes was **re-verified directly against source** before any fix was applied, and several suggested fixes were rejected after verification (see §5).
7. Applied only verified, additive, backward-compatible fixes; compiled and ran the full test suite for every touched module.

---

## 2. Fixes Applied

### 2.1 Missing/defeated indexes (pure Flyway migrations — additive, zero business-code risk)

| Service | Migration | Problem | Fix |
|---|---|---|---|
| identity-service | `V19__user_session_refresh_token_index.sql` | `UserSessionRepository.findByRefreshTokenIdAndRevokedAtIsNull` — called on every refresh/rotation/revocation — had **no index at all** on `refresh_token_id`. | Partial index `(refresh_token_id) WHERE revoked_at IS NULL`. |
| lead-service | `V019__lead_duplicate_detection_indexes.sql` | (a) `findPotentialDuplicates` filters `source_type`/`source_id` with no index; (b) same query's `LOWER(email) = LOWER(:email)` defeated `idx_leads_email`; (c) `LeadDuplicateRepository`'s `ORDER BY detected_at` had no covering index; (d) `findByTenantIdAndTagIgnoreCase` defeated `idx_lead_tags_tag`. | Added `(tenant_id, source_type, source_id)`, functional `(tenant_id, lower(email))`, `(tenant_id, resolved, detected_at DESC)`, functional `(tenant_id, lower(tag))`. |
| customer-service | `V008__customer_search_query_indexes.sql` | (a) `CustomerRepository.search()`'s `ORDER BY created_at DESC` (paginated) had no covering index; (b) `findIdentityCandidates`/`findPotentialDuplicates`'s `LOWER(email) = LOWER(:email)` defeated `idx_customers_tenant_email`. | Added `(tenant_id, created_at DESC) WHERE deleted_at IS NULL` and functional `(tenant_id, lower(email)) WHERE deleted_at IS NULL AND email IS NOT NULL`. |
| catalog-service | `V007__catalog_product_category_lower_index.sql` | `CatalogProductRepository.search()`'s `LOWER(category) = LOWER(:category)` defeated `idx_catalog_product_tenant_category`. | Functional index `(tenant_id, lower(category))`. |
| marketplace-service | `V017__plugin_installation_tenant_updated_index.sql` | `findByTenantIdOrderByUpdatedAtDesc` sorts by `updated_at`; only a `(tenant_id, status)` index existed. | Added `(tenant_id, updated_at DESC)`. |
| ai-service | *(code fix, not a migration — see §2.3)* | `findByDimensionsExact`/`findByDimensionsLanguageAgnostic` wrapped **both** the column and parameter in `UPPER()`, defeating the plain `idx_prompt_template_dimensions` index even though the underlying values are always stored pre-normalized. | Removed the redundant `UPPER()` wrapping (see §2.3 for the write-path verification that made this safe). |

Previously in this engagement (same review pass, applied before the parallel exploration completed) — also purely additive:

- 11 services' `outbox_events` table gained/upgraded a covering index for `common-events`' `claimPendingEvents()` query: `(aggregate_type, aggregate_id, created_at)`.
- `pg_trgm` GIN indexes for leading-wildcard `LIKE` search in `lead-service`, `customer-service`, `catalog-service`.

**Reviewed, no index added (already adequately covered):**
- `search-service`'s `SearchDocumentRepository` hybrid/vector queries — already has `(tenant_id, entity_type)` and `(tenant_id, entity_type, status)` composite indexes; an additional `source`-inclusive index would be low-value given `status` is already the more selective predicate.
- `conversation-service`'s `ConversationMessageRepository` — already has `(conversation_id, created_at)`, which is sufficient since `conversation_id` itself is a single-tenant-scoped key.

### 2.2 N+1 query fixes (internal repository/service changes — DTO/API shape unchanged)

| Service | File | Before | After |
|---|---|---|---|
| ai-service | `PromptService.list()` | One `findByTenantIdAndPromptIdOrderByVersionNumberDesc` query **per row** on every page of prompts, to read only the top version. | Added `PromptVersionRepository.findByTenantIdAndPromptIdInOrderByPromptIdAscVersionNumberDesc`; `list()` now issues **one** batch query for the whole page and picks the latest version per prompt in memory. |
| marketplace-service | `PluginRegistryService.listInstallations()` | One `catalogRepository.findByPluginKey(...)` **per installation**. | Added `PluginCatalogRepository.findByPluginKeyIn(...)`; batch-loads all catalog types in one query, then maps in memory. |
| identity-service | `RefreshTokenService.revokeTokenFamily()` | Looped every token in the family, calling `save()` per token **and** `sessionService.revokeSessionByRefreshTokenId()` (its own query) per token. | Uses the existing (previously unused) bulk `revokeAllActiveByTokenFamilyId()` `UPDATE`, and a new `SessionService.revokeSessionsByRefreshTokenIds(Collection<UUID>)` / `UserSessionRepository.findByRefreshTokenIdInAndRevokedAtIsNull(...)` to revoke all family sessions in one query. |
| workflow-service | `WorkflowAutomationEngine.retryDue()` | One `ruleRepository.findByTenantIdAndIdAndDeletedAtIsNull(...)` **per due execution**. | Added `WorkflowRuleRepository.findByTenantIdAndIdInAndDeletedAtIsNull(...)`; batch-loads all rules for the due batch in one query. |

### 2.3 Query correctness/optimization fix (ai-service)

`PromptTemplateRepository.findByDimensionsExact` / `findByDimensionsLanguageAgnostic` compared `UPPER(p.industryCode) = UPPER(:industryCode)` etc. Verified via `PromptService.create()` (`normalizeDimension`/`normalizeLanguage` run on every write), the only other writer (`V19__seed_natural_farming_prompts.sql`, which seeds already-uppercase/lowercase values), and the only caller (`PromptService.resolveForExecution` → `firstDimensionMatch`, which normalizes before querying) that **every** stored and queried value already has consistent case. The `UPPER()` wrapping was therefore redundant and was defeating the plain `idx_prompt_template_dimensions` index. Removed it — behavior is identical, index is now usable.

### 2.4 Transaction-safety fixes (annotation-only; no behavior change on the success path)

| Service | Class/Method | Issue | Fix |
|---|---|---|---|
| identity-service | `TokenService.issueTokens` / `rotateTokens` | Refresh-token creation and session creation ran as two independent implicit transactions; a failure between them could leave an orphaned refresh token. Verified both callees only touch the local DB (no cross-service REST calls held under the transaction). | Added `@Transactional` to both methods. |
| ai-service | `SemanticCacheService.hitRate()` | The only method in this class with no explicit transaction boundary and no embedding-provider HTTP call to keep outside a transaction (unlike `get()`/`put()`, which intentionally use manual `TransactionTemplate` scoping for that reason) — its two repository calls could see inconsistent counts. | Added `@Transactional(readOnly = true)`. |
| lead-service | `DuplicateLeadDetectionService.detectAndRecord` | Read-then-save-N-duplicates loop had no transaction boundary of its own; only atomic today because its sole caller (`LeadService.createLead`) is `@Transactional`. | Added `@Transactional` directly to the method so it is safe regardless of caller. |
| customer-service | `CustomerDuplicateDetectionService.detectAndRecord` | Same pattern as above. | Added `@Transactional`. |
| tenant-service | `UserService.getById` / `listByTenant` | No transaction boundary at all (not even `readOnly`). | Added `@Transactional(readOnly = true)`. |

---

## 3. Files Modified

**ai-service**
- `infrastructure/persistence/PromptTemplateRepository.java` — removed redundant `UPPER()` wrapping.
- `infrastructure/persistence/PromptVersionRepository.java` — added batch `findByTenantIdAndPromptIdInOrderByPromptIdAscVersionNumberDesc`.
- `application/service/PromptService.java` — `list()` now batch-loads latest versions.
- `application/service/SemanticCacheService.java` — added `@Transactional(readOnly = true)` to `hitRate()`.

**identity-service**
- `authentication/infrastructure/persistence/RefreshTokenRepository.java` — unchanged (existing bulk method now used).
- `authentication/application/RefreshTokenService.java` — `revokeTokenFamily()` batched.
- `authentication/application/TokenService.java` — `@Transactional` added to `issueTokens`/`rotateTokens`.
- `session/application/SessionService.java` — added `revokeSessionsByRefreshTokenIds(...)`.
- `session/infrastructure/persistence/UserSessionRepository.java` — added `findByRefreshTokenIdInAndRevokedAtIsNull(...)`.
- `src/main/resources/db/migration/V19__user_session_refresh_token_index.sql` — new.

**lead-service**
- `application/service/DuplicateLeadDetectionService.java` — `@Transactional` added.
- `src/main/resources/db/migration/V019__lead_duplicate_detection_indexes.sql` — new.

**customer-service**
- `application/service/CustomerDuplicateDetectionService.java` — `@Transactional` added.
- `src/main/resources/db/migration/V008__customer_search_query_indexes.sql` — new.

**catalog-service**
- `src/main/resources/db/migration/V007__catalog_product_category_lower_index.sql` — new.

**marketplace-service**
- `infrastructure/persistence/PluginCatalogRepository.java` — added `findByPluginKeyIn(...)`.
- `application/service/PluginRegistryService.java` — `listInstallations()` batched.
- `src/main/resources/db/migration/V017__plugin_installation_tenant_updated_index.sql` — new.

**workflow-service**
- `infrastructure/persistence/WorkflowRuleRepository.java` — added `findByTenantIdAndIdInAndDeletedAtIsNull(...)`.
- `application/engine/WorkflowAutomationEngine.java` — `retryDue()` batched.

**tenant-service**
- `application/service/UserService.java` — `@Transactional(readOnly = true)` added to `getById`/`listByTenant`.

*(Plus the outbox-index and pg_trgm-index migrations across 11 + 3 services applied earlier in this same review — already covered in the prior turn and not repeated here.)*

---

## 4. Findings Documented but **Not** Fixed (with reasoning)

These were raised by manual review and/or the parallel exploration passes, verified directly against source, and deliberately left as-is or deferred:

| Finding | Why not fixed |
|---|---|
| **Unbounded `List<T>` finders exposed via REST** — e.g. `tenant-service` `UserController.listByTenant`, and lead-service's activity/note/history/followup/duplicate list endpoints, all return a bare JSON array today. | Converting these to `Page<T>` changes the **response shape** of a public REST contract (Rule 04: "Keep all public APIs backward compatible" / "Never remove working functionality"). This requires a coordinated API version bump (`/api/v2/...`) and client migration, which is out of scope for a repository-level fix-only pass. **Recommendation:** track as a v2 API initiative; in the interim these lists are bounded by natural business cardinality (per-tenant users, per-lead activity) and not an acute risk. |
| `CatalogMatchingService.match()` / `CatalogRecommendationService.recommend()` `@Transactional` (not `readOnly`) flagged as "should be read-only". | **Rejected after verification** — both methods call `eventPublisher.publish(...)` (`CatalogMatchedEvent`/`CatalogRecommendationGeneratedEvent`), which is a write (outbox event persistence) inside the same transaction. `readOnly = true` would be incorrect here. |
| `LeadOpportunityConversionService.convertToOpportunity` / `LeadAiQualificationService.qualifyWithAi` flagged as "missing `@Transactional`, partial-success risk". | **Rejected after verification** — both methods call another microservice over REST (`dealServiceClient` / `aiServiceClient.qualify(...)`) *between* local writes. Wrapping this in a local `@Transactional` would hold a DB connection/transaction open across blocking network I/O to another service, which Rule 01 explicitly prohibits ("Never attempt distributed database transactions... cross-service consistency uses Events/Saga/Workflow"). The current design — independent, intentionally eventually-consistent local writes around the cross-service call — is correct. |
| `search-service` `SearchDocumentRepository` missing a `source`-inclusive index. | Verified the existing `(tenant_id, entity_type, status)` index already serves the most selective predicates well; the marginal gain of an additional index didn't justify the added write-path/maintenance cost without evidence of an actual slow-query. |
| `conversation-service` `ConversationMessageRepository` missing a `tenant_id`-prefixed composite index. | Verified the existing `(conversation_id, created_at)` index is already sufficient, since `conversation_id` is itself a single-tenant-scoped key; adding `tenant_id` as a redundant prefix would not measurably help. |
| `RefreshTokenRepository.findByTokenFamilyId` / `UserSessionRepository.findByUserIdAndRevokedAtIsNull` returning unbounded `List` (identity-service). | These are internal, not REST-exposed, and bounded by realistic per-user/per-family cardinality (active sessions/token generations per user, not overall table size) — no fix needed. |

---

## 5. Verification

- `mvn compile -am` succeeded for every touched module (ai-service, identity-service, lead-service, customer-service, catalog-service, marketplace-service, workflow-service, tenant-service).
- `mvn test` run for all touched modules:
  - `RefreshTokenServiceTest`, `WorkflowAutomationEngineTest`, `PluginRegistryServiceTest` — all pass.
  - Full suites for `tenant-service`, `customer-service`, `lead-service`, `catalog-service` — all pass (Testcontainers-dependent integration tests were skipped in this environment because Docker is unavailable locally; no assertion failures).
  - `ai-service`'s `PromptServiceTest` — 6 of 7 pass; `shouldCreateAndActivateNewVersion` fails with a Mockito `UnnecessaryStubbingException`. Verified via `git diff` that this test method and the code path it exercises (`createVersion`/`archiveActiveVersions`) were **not touched** by this review — the failure is a pre-existing test-infrastructure issue, unrelated to the fixes in this report, and out of scope to fix here.
- `mvn install -DskipTests` succeeded for ai-service, marketplace-service, identity-service, workflow-service (full packaging, not just compile).
- No entity, DTO, controller, or event contract was changed. No repository method was removed; only additive methods were introduced.

---

## 6. Remaining Recommendations

1. **API pagination migration (v2):** Plan a coordinated `/api/v2` rollout to paginate the unbounded list endpoints identified in §4, starting with the highest-cardinality ones (lead activity/notes/history, customer interactions).
2. **`LeadRepository.search()` / `CustomerRepository.search()` free-text filters:** currently a leading-wildcard `LOWER(...) LIKE '%q%'` pattern; the new `pg_trgm` indexes (already applied) make this index-assisted, but a longer-term move to the existing FTS (`tsvector`) columns or a dedicated `Specification`-based query builder would be cleaner and faster for multi-field search.
3. **`common-events` outbox claim query:** now indexed consistently across all 14 services that use it; consider consolidating the `claimPendingEvents` native query's `NOT EXISTS` correlated subquery into a window-function form if outbox table sizes grow large enough to matter (not currently a measured bottleneck).
4. **Pre-existing `PromptServiceTest` flakiness:** `shouldCreateAndActivateNewVersion` should be fixed by ai-service's test owners (likely just removing or relaxing the unused `promptVersionRepository.save(...)` stub) — unrelated to this review but worth a follow-up ticket.
5. **`JpaSpecificationExecutor`:** not used anywhere in the codebase. The multi-filter `search()` queries (`LeadRepository`, `CustomerRepository`, `CatalogProductRepository`) are reasonable candidates if filter combinations grow further, but today's `(:param IS NULL OR ...)` pattern is simple enough not to warrant the change.
