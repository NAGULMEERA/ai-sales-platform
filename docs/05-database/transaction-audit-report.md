# Transaction Audit Report

**Status:** Completed
**Scope:** Every `@Transactional` method across `backend/common/common-events` and all 18 `backend/services/*` microservices (identity, tenant, lead, customer, catalog, conversation, appointment, marketplace, media, notification, ai, analytics, audit, billing, search, workflow, integration, deal).
**Checklist applied to every method:** propagation, rollback behavior, `readOnly` correctness, nested/self-invocation transactions, event publishing relative to commit, and transaction boundaries (in particular: DB transactions held open across network I/O — Feign calls, S3, embedding providers).
**Public APIs changed:** None. Every fix is internal to service/application-layer implementation (annotations, `TransactionTemplate` usage, one new repository method per idempotency table, one new package-private batching method). No DTO, REST contract, or entity shape was changed.

---

## 1. Method

1. Mapped the platform's event-publishing infrastructure first, since "event publishing after commit" cannot be judged without it: `OutboxEventPublisher` (the default — `Propagation.MANDATORY`, writes to the local `outbox_events` table inside the caller's transaction, a separate poller publishes to Kafka after commit) vs. `KafkaEventPublisher` (direct publish, used only where the outbox is explicitly disabled). Confirmed which services run with the outbox enabled (all business services) vs. disabled.
2. Reviewed the shared transactional building blocks in `common-events` (`OutboxClaimService`, `InboxService`, `DeadLetterService`, `ProcessedEventRepository`) that every consumer relies on — found these already correct (`InboxService`/outbox claim use `Propagation.MANDATORY`/`REQUIRED` correctly scoped to the caller's transaction; `DeadLetterService` correctly uses `REQUIRES_NEW` so a dead-letter record survives the rollback of the failed handler).
3. Ran parallel exploration passes grouped by service to inventory every `@Transactional` method, its propagation/`readOnly` setting, what it calls (local repositories only vs. Feign/S3/AI-provider/Kafka), and whether any method throws *after* a side-effecting call in the same transaction (rollback-safety) or is invoked via self-invocation (bypasses the Spring AOP proxy, silently dropping the annotation).
4. Every finding from those passes was **re-verified directly against source** before any fix was applied — several suspected issues were rejected after verification (see §4).
5. Applied only verified, additive, backward-compatible fixes; compiled and ran the full test suite for every touched module.

---

## 2. Fixes Applied

### 2.1 Critical — rollback silently undoes a security action (identity-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `RefreshTokenService.revokeTokenFamily(UUID)` | Called from `AuthService.refresh`/`handleInvalidOrReuse` (reuse-detection) as part of a larger `@Transactional` method that **throws immediately after** calling it (to reject the request). Because the revocation shared the caller's transaction, an exception thrown after the revoke call rolled the whole transaction back — **silently undoing the security-critical token-family revocation** it was supposed to perform before rejecting the reused/replayed token. | Refactored to use a manual `TransactionTemplate` with `Propagation.REQUIRES_NEW`, so the revocation commits **independently and immediately**, regardless of what the caller does afterward. |

### 2.2 Critical — aborted-transaction pattern on concurrent idempotency-key inserts (lead, customer, deal, tenant services)

| Service | Class | Issue | Fix |
|---|---|---|---|
| lead-service | `LeadIdempotencyService.storeCreateResponse` | Caught `DataIntegrityViolationException` on the unique-constraint violation from a concurrent `save()`, then tried `findCached(...)` **in the same transaction**. On PostgreSQL, a failed `INSERT` poisons the current transaction — every subsequent statement (including the `SELECT` in `findCached`) fails with `current transaction is aborted, commands ignored until end of transaction block`, so the "safe" fallback path itself always failed under real concurrency. | Added `IdempotencyKeyRepository.insertIgnoreConflict(...)` — a native `INSERT ... ON CONFLICT (idempotency_key) DO NOTHING` — which never aborts the transaction on conflict. `storeCreateResponse` now checks the affected-row count and only falls back to `findCached(...)` (now safe to run) when the insert reports `0` rows affected. |
| customer-service | `CustomerIdempotencyService.storeCreateResponse` | Same pattern. | Same fix. |
| deal-service | `QuoteIdempotencyService.storeCreateResponse` | Same pattern. | Same fix. |
| tenant-service | `TenantIdempotencyService.storeCreateResponse` | Same pattern. | Same fix. |

### 2.3 Critical — same aborted-transaction pattern in AI quota tracking (ai-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `AiQuotaService.lockOrCreateBudgetDay` | Caught `DataIntegrityViolationException` on a concurrent `TenantAiBudgetDay` row creation, then called `findForUpdate(...)` in the same (now-aborted) transaction — identical failure mode to §2.2, on the hot path that gates every AI request's token budget. | Added `TenantAiBudgetDayRepository.insertIgnoreConflict(...)` (`INSERT ... ON CONFLICT (tenant_id, usage_day) DO NOTHING`); `lockOrCreateBudgetDay` now inserts-or-ignores unconditionally, then always does the `findForUpdate(...)` lock read, which is now guaranteed to run in a healthy transaction. |

### 2.4 Critical — event published without a guaranteed active transaction (workflow-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `WorkflowActionExecutor.sendNotification` / `publishCoordinationEvent` | Both call `eventPublisher.publish(...)`, which (via `OutboxEventPublisher`) requires `Propagation.MANDATORY` — an already-active transaction, or it throws `IllegalTransactionStateException`. `WorkflowActionExecutor` itself has no `@Transactional` anywhere. Today this only works because its sole caller path (`WorkflowAutomationEngine.onTrigger` ← Kafka `@KafkaListener` ← `IntegrationEventListener`/inbox, see §4) happens to wrap every invocation in a transaction — a coincidence of the current call graph, not a guarantee enforced by this class. Any future caller of this executor without that specific inbox-wrapped path (e.g. a direct REST-triggered action, or a scheduled retry) would fail immediately. | Injected `PlatformTransactionManager` and added a `publishInTransaction(BaseEvent)` helper using `TransactionTemplate` (default `REQUIRED` propagation): it **joins** the caller's transaction when one is active (identical behavior to today) or **opens one** when none exists, so the `MANDATORY` requirement is always satisfied regardless of caller. |

### 2.5 Moderate — non-atomic audit-trail writes after a multi-service conversion flow (lead-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `LeadOpportunityConversionService.convertToOpportunity` | Unlike every other method in `LeadService`/`LeadJourneyService`/etc. (all `@Transactional`, see the codebase-wide pattern), this method has **no** transaction boundary of its own — correctly so, since it makes several Feign calls to `catalogServiceClient`/`dealServiceClient` and must not hold a DB transaction open across that network I/O (Rule 01/05). However, after those calls complete, it recorded 2–3 audit-trail activities (`OPPORTUNITY_CREATED`, `CATALOG_MATCHED`, `RECOMMENDATION_GENERATED`) via three **separate** `sideEffects.recordActivity(...)` calls, each auto-committing independently (Spring Data's per-method transaction on `save()`). A crash between calls would leave a partial audit trail for a single logical outcome. | Added `LeadSideEffectRecorder.recordActivities(UUID, UUID, List<ActivityEntry>)`, a `@Transactional` batch method (`saveAll`) that persists all activities for one outcome atomically. `LeadOpportunityConversionService` now builds the activity list after all external calls finish and makes one call. External calls remain correctly outside any local transaction — only the pure-DB audit writes were batched. |

### 2.6 Moderate — document permanently stuck in `INDEXING` on quota/provider failure (ai-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `DocumentEmbeddingPipeline.index` | Claims the document (sets status to `INDEXING` in its own short transaction) via `claimForIndexing`, then calls `embeddingProviderRegistry.resolveDefault()` and `aiQuotaService.reserveEmbed(tenantId)` **before** entering the `try` block that calls `markFailedIfClaimHeld(...)` on failure. If either of those two calls throws (e.g. no embedding provider configured, or the tenant's AI budget is exhausted), the claim is never released — the document is left in `INDEXING` forever, and any retry is rejected by `claimForIndexing`'s own "already being indexed" guard. | Restructured so **everything** after the claim (`resolveDefault`, `reserveEmbed`, the embed call, chunk persistence) runs inside a single `try` at the top level of `index(...)`, with one `catch (RuntimeException ex) { markFailedIfClaimHeld(...); throw ex; }` around all of it. `markFailedIfClaimHeld` is itself idempotent (no-ops if the claim was already lost to a peer), so this is safe to call for any failure cause. The AI-quota reservation/release pairing and the main persistence `TransactionTemplate` are otherwise unchanged. |

### 2.7 Moderate — DB transaction held open across an S3 upload (media-service)

| Class/Method | Issue | Fix |
|---|---|---|
| `MediaApplicationService.upload` | Annotated `@Transactional`, but calls `storage.put(objectKey, bytes, contentType)` — a network call to the object store — **inside** that transaction, holding a DB connection open for the entire upload duration (Rule 01/05: never hold a DB connection across external I/O; also a connection-pool-exhaustion risk under load). The method performs exactly one DB write (`mediaObjectRepository.save(...)`) after the upload completes. | Removed `@Transactional` from `upload(...)`. The single `save()` call is already atomic on its own via Spring Data JPA's per-method transaction (`SimpleJpaRepository`); no atomicity is lost, and the S3 call no longer holds a DB connection. `get`/`downloadContent` (already `@Transactional(readOnly = true)`, no S3 call inside a write path) are unchanged. |

---

## 3. Files Modified

**identity-service**
- `authentication/application/RefreshTokenService.java` — `revokeTokenFamily` now runs in an independent `REQUIRES_NEW` transaction; constructor gained a `PlatformTransactionManager` dependency.
- `src/test/java/.../RefreshTokenServiceTest.java` — updated constructor call to supply a mocked `PlatformTransactionManager`.

**lead-service**
- `infrastructure/persistence/IdempotencyKeyRepository.java` — added `insertIgnoreConflict(...)`.
- `application/service/LeadIdempotencyService.java` — `storeCreateResponse` uses `insertIgnoreConflict`; removed the `DataIntegrityViolationException` catch.
- `application/service/LeadSideEffectRecorder.java` — added `recordActivities(UUID, UUID, List<ActivityEntry>)` (`@Transactional`) and the `ActivityEntry` record.
- `application/service/LeadOpportunityConversionService.java` — batches the post-conversion activity writes into one `recordActivities(...)` call.
- `src/test/java/.../LeadOpportunityConversionServiceTest.java` — updated assertion to verify the batched call instead of the removed per-activity calls.

**customer-service**
- `infrastructure/persistence/IdempotencyKeyRepository.java` — added `insertIgnoreConflict(...)`.
- `application/service/CustomerIdempotencyService.java` — same fix as lead-service.

**deal-service**
- `infrastructure/persistence/IdempotencyKeyRepository.java` — added `insertIgnoreConflict(...)`.
- `application/service/QuoteIdempotencyService.java` — same fix.

**tenant-service**
- `infrastructure/persistence/IdempotencyKeyRepository.java` — added `insertIgnoreConflict(...)`.
- `application/service/TenantIdempotencyService.java` — same fix.
- `src/test/java/.../TenantIdempotencyServiceTest.java` — updated `shouldStoreCreateResponse`/`shouldIgnoreDuplicateKeyWhenResponseAlreadyStored`/`shouldIgnoreBlankKey` to stub/verify `insertIgnoreConflict` instead of `save(...)`.

**ai-service**
- `infrastructure/persistence/TenantAiBudgetDayRepository.java` — added `insertIgnoreConflict(...)`.
- `application/service/AiQuotaService.java` — `lockOrCreateBudgetDay` uses `insertIgnoreConflict`; removed the `DataIntegrityViolationException` catch.
- `application/rag/DocumentEmbeddingPipeline.java` — extracted `doIndex(...)`; `index(...)` now wraps the entire post-claim flow (provider resolution, quota reservation, embedding call, persistence) in one `try/catch` that always releases the `INDEXING` claim on failure.

**workflow-service**
- `application/engine/WorkflowActionExecutor.java` — added `PlatformTransactionManager` dependency and a `publishInTransaction(BaseEvent)` helper; both event-publishing call sites now use it.
- `src/test/java/.../WorkflowActionExecutorTest.java` — updated constructor call to supply a mocked `PlatformTransactionManager`.

**media-service**
- `application/service/MediaApplicationService.java` — removed `@Transactional` from `upload(...)`.

---

## 4. Findings Documented but **Not** Fixed (with reasoning)

| Finding | Why not fixed |
|---|---|
| **`WorkflowAutomationEngine.onTrigger`/`retryDue` have no `@Transactional` of their own** — the `MANDATORY` requirement satisfied in §2.4 today relies on the fact that `onTrigger` is only ever reached through `WorkflowAutomationEventConsumer`'s `@KafkaListener` → `IntegrationEventListener`/inbox transaction (`common-events`, confirmed `@Transactional`/`Propagation.MANDATORY`-scoped correctly there). | Adding `@Transactional` directly to `onTrigger` would be **incorrect**, not just redundant: `WorkflowActionExecutor.execute(...)` also handles `ASSIGN_LEAD`/`UPDATE_LEAD`/`CREATE_OPPORTUNITY`/`CALL_AI_GATEWAY` actions, which make **Feign calls to other services** from inside the same rule-evaluation loop. Wrapping the whole engine in `@Transactional` would hold a DB transaction open across that network I/O — the exact anti-pattern Rule 01/05 prohibit, and one this report fixes elsewhere (§2.5, §2.7). The surgical fix in §2.4 (transaction only around the `publish()` call itself) is the correct scope. **Recommendation:** if `onTrigger`/`retryDue` ever gain a non-inbox-wrapped caller (direct REST trigger, unwired scheduled path), each `WorkflowActionExecutor` call site that persists (not just `publish`) should be reviewed the same way — but no such caller exists today, so there is nothing further to fix now. |
| **Self-invocation on `@Transactional` overloads** — e.g. `TenantService.createTenant(request)` calls `createTenant(request, idempotencyKey)` on `this`; both are annotated `@Transactional`. | Harmless today: the *outer* overload's `@Transactional` proxy still applies (it's invoked externally, through the Spring proxy), so the whole call executes in one `REQUIRED` transaction regardless of whether the inner overload's annotation is honored via the bypassed self-invocation. The inner annotation is redundant, not incorrect. Not changed — refactoring to remove the redundant annotation is cosmetic and carries a small risk of missing a future caller that invokes the two-arg overload directly from outside the class (where the annotation *would* matter); left as-is. |
| **`common-events` `OutboxClaimService`, `InboxService`, `DeadLetterService`, `ProcessedEventRepository`** — reviewed for propagation/rollback correctness since every consumer in the platform depends on them. | No issues found. `InboxService` correctly uses `Propagation.MANDATORY` (must run inside the caller's/listener's transaction) with an `insertIgnoreConflict`-style idempotent claim (same safe pattern applied in §2.2/§2.3). `DeadLetterService` correctly uses `Propagation.REQUIRES_NEW` so a dead-letter record is preserved even when the failed handler's transaction rolls back. No changes needed. |
| **`CatalogMatchingService.match()` / `CatalogRecommendationService.recommend()`** and **`LeadAiQualificationService.qualifyWithAi()`** flagged by the exploration passes as "possible missing/incorrect transaction boundary". | **Re-verified and rejected** (these were also flagged and rejected in the prior repository-review pass for the same underlying reason): both publish an outbox event as a local write ruling out `readOnly = true`, and/or call another microservice (`aiServiceClient`) between local writes, ruling out wrapping the whole method in one local `@Transactional` (would hold a DB transaction across network I/O). Current design — local writes transactional, cross-service calls intentionally outside any local transaction, consistency handled via events — is correct per Rule 01/03. |

---

## 5. Verification

- `mvn test-compile -am` for every touched module (identity-service, lead-service, customer-service, deal-service, tenant-service, ai-service, workflow-service, media-service) — succeeds.
- `mvn test` targeted at every test class touching the changed production code (`RefreshTokenServiceTest`, `LeadOpportunityConversionServiceTest`, `TenantIdempotencyServiceTest`, `QuoteIdempotencyServiceTest`, `WorkflowActionExecutorTest`, and the full suites below) — all pass.
- Full `mvn test` suite per touched module:
  - identity-service, lead-service, customer-service, deal-service, tenant-service, workflow-service, media-service — **all pass** (Testcontainers-dependent integration tests are skipped in this environment because Docker is unavailable locally; no assertion failures in any test that ran).
  - ai-service — 84 of 86 tests pass. The 2 failures are **pre-existing and unrelated** to this audit, confirmed via `git diff`/`git show HEAD`:
    - `PromptServiceTest.shouldCreateAndActivateNewVersion` — Mockito `UnnecessaryStubbingException` in `createVersion()`/`archiveActiveVersions()`, a code path untouched by this or the prior review.
    - `KnowledgeIndexingServiceTest.shouldIndexTextAndMarkDocumentReady` — asserts `knowledgeChunkVectorRepository.updateEmbedding(...)` (singular) was called, but `DocumentEmbeddingPipeline` has always called `updateEmbeddings(...)` (plural/batch, confirmed unchanged at `HEAD` via `git show`) — a stale test expectation predating this session's changes.
- No entity, DTO, controller, or event contract was changed. No REST endpoint signature changed. Every repository change is a new, additive method (`insertIgnoreConflict`); no existing method was removed or altered.

---

## 6. Remaining Recommendations

1. **Fix the two pre-existing ai-service test failures** documented in §5 (owner: ai-service test maintainers) — both are test-expectation bugs unrelated to production behavior, not caused by this audit.
2. **`WorkflowAutomationEngine` transaction boundary** (§4): if a non-inbox-wrapped trigger path for `onTrigger`/`retryDue` is ever added, re-audit every `WorkflowActionExecutor` action type at that time, not just the event-publishing ones fixed here.
3. **Generalize the `insertIgnoreConflict` idempotent-insert pattern**: four services (lead, customer, deal, tenant) plus ai-service's budget-day table now each have a hand-rolled native `INSERT ... ON CONFLICT DO NOTHING` repository method for the same purpose `InboxService`/`ProcessedEventRepository` already solved in `common-events`. A shared `common-core`/`common-events` idempotent-insert helper (e.g. a small JPQL/native-query snippet builder, or documenting the pattern in `common-testing`/developer guide) would prevent future services from re-introducing the `DataIntegrityViolationException`-catch anti-pattern this report fixed in five places.
4. **Redundant `@Transactional` on self-invoked overloads** (§4): low-priority cleanup opportunity (e.g. `TenantService.createTenant(request)`/`(request, key)`) — remove the inner annotation or restructure as a private non-transactional helper, purely for clarity; no behavior change.
5. **Consider a lightweight ArchUnit rule** (the platform already has an `architecture-tests` module) asserting that no class calling `EventPublisher.publish(...)` is reachable from a public entry point without an enclosing `@Transactional`/`TransactionTemplate` — would have caught the §2.4 issue automatically and prevents regressions as new workflow actions are added.
