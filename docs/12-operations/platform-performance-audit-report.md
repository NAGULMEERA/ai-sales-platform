# Platform Performance Audit Report

**Date:** 2026-08-05  
**Scope:** SQL, memory, cache/Redis, batch operations, lazy loading, query optimization, thread pools, async processing  
**Mode:** Verified bottlenecks only; public APIs kept backward compatible; no breaking schema changes

---

## Executive summary

Audits confirmed several production-impacting hotspots (Redis `KEYS`, per-row embedding updates, duplicate-detection N+1 writes, unbounded AI conversation context, unbounded embedding HTTP batches, uncapped workflow retries). Targeted fixes were applied. Thread pools / `@Async` / Kafka concurrency were reviewed and found bounded or intentional.

| Area | Verified issues | Fixed this pass |
|------|-----------------|-----------------|
| Redis / cache | KEYS-based eviction | Yes (SCAN + batched delete) |
| SQL / batch | Per-chunk embedding UPDATE; duplicate exists+save loops | Yes |
| Memory / query bounds | Unbounded AI context messages; uncapped duplicate candidates; uncapped workflow retries | Yes |
| Embedding pipeline | Single giant provider call | Yes (slices of 64) |
| Lazy loading / OSIV | Open-in-view already false; Hibernate batch props present | N/A (already healthy) |
| Thread pools / async | No unbounded prod pools | N/A |
| Kafka inbox TX + Feign/SMTP | Confirmed high-risk pattern | Deferred (see recommendations) |

---

## Changes made

### 1. Redis eviction: KEYS → SCAN (Critical)

**Problem:** `PlatformCacheService.evictByPattern` used `redisTemplate.keys(pattern)`, which blocks Redis under large keyspaces (RBAC cache clear, semantic-cache tenant invalidate, tenant-wide eviction).

**Fix:** Cursor-based `SCAN` with count 256; delete in batches of 256.

**Files:**
- `backend/common/common-cache/src/main/java/com/aisales/common/cache/PlatformCacheService.java`
- `backend/common/common-cache/src/test/java/com/aisales/common/cache/PlatformCacheServiceTest.java`

### 2. Embedding vector writes: JDBC batch (High)

**Problem:** `KnowledgeChunkVectorRepository.updateEmbeddings` issued one native `UPDATE` per chunk.

**Fix:** Hibernate `Session.doWork` + JDBC `PreparedStatement.addBatch` / `executeBatch` (batch size 50), still tenant-scoped (`WHERE id = ? AND tenant_id = ?`).

**Files:**
- `backend/services/ai-service/src/main/java/com/aisales/ai/infrastructure/persistence/KnowledgeChunkVectorRepository.java`

### 3. Embedding provider calls: slice large docs (High)

**Problem:** `DocumentEmbeddingPipeline` sent all chunks in one `embedWithUsage` call (timeout / heap risk on large docs).

**Fix:** Embed in slices of 64; aggregate vectors/tokens; persist path unchanged.

**Files:**
- `backend/services/ai-service/src/main/java/com/aisales/ai/application/rag/DocumentEmbeddingPipeline.java`

### 4. Customer duplicate detection: batch read + `saveAll` (High)

**Problem:** Per-match `existsBy…` + per-match `save` on create/update; unbounded candidate query.

**Fix:** Cap candidates at 50 (`Pageable`); one query for unresolved counterpart IDs; `saveAll`.

**Files:**
- `backend/services/customer-service/.../CustomerDuplicateDetectionService.java`
- `backend/services/customer-service/.../CustomerDuplicateRepository.java`
- `backend/services/customer-service/.../CustomerRepository.java` (`findPotentialDuplicates` + `Pageable`)

### 5. Lead duplicate detection: `saveAll` + candidate cap (High)

**Problem:** Unbounded match query + `save` in a loop on lead create.

**Fix:** Cap at 50; `saveAll`.

**Files:**
- `backend/services/lead-service/.../DuplicateLeadDetectionService.java`
- `backend/services/lead-service/.../LeadRepository.java`
- `backend/services/lead-service/.../DuplicateLeadDetectionServiceTest.java`

### 6. Conversation AI context: last-N window (High)

**Problem:** `ConversationContextService.loadContext` loaded entire message history into memory/payload.

**Fix:** Load newest 100 messages via `findRecentByTenantIdAndConversationId` + reverse; full history still available via existing `listMessages` API (unchanged contract).

**Files:**
- `backend/services/conversation-service/.../ConversationMessageRepository.java`
- `backend/services/conversation-service/.../ConversationContextService.java`

### 7. Workflow retryDue: batch cap (Medium)

**Problem:** Loaded all due `RETRYING` executions with no limit.

**Fix:** `PageRequest` limit 100 per invocation.

**Files:**
- `backend/services/workflow-service/.../WorkflowAutomationExecutionRepository.java`
- `backend/services/workflow-service/.../WorkflowAutomationEngine.java`

---

## Design decisions

1. **No API contract breaks** — list endpoints that still return full lists were left alone where clients may depend on them; AI context was windowed because it is an internal enrichment path.
2. **Inbox TX + remote I/O not “fixed” yet** — `InboxService.tryClaim` is intentionally `MANDATORY` so claim rolls back with handler failure. Splitting claim from Feign/SMTP requires a processing/complete state (or reclaim-on-failure) redesign; documented as recommendation rather than a risky one-line change.
3. **Reuse existing infrastructure** — SCAN uses Spring Data Redis APIs already on the classpath; no new dependencies.
4. **Preserve tenant isolation** — all SQL batch paths retain `tenant_id` predicates.

---

## Already healthy (verified, no change)

| Check | Result |
|-------|--------|
| `spring.jpa.open-in-view` | `false` across services + shared observability config |
| Hibernate JDBC batch | `batch_size=50`, `order_inserts/updates=true` via `application-observability.yml` |
| Production `findAll()` | Not used on hot paths (tests only) |
| `@Async` / unbounded `CachedThreadPool` | Not present in production code |
| Outbox dispatcher | Batched claim + bounded parallelism |
| Kafka listener concurrency | Default 1 (configurable, not extreme) |

---

## Remaining recommendations (not changed)

| Priority | Item | Why deferred |
|----------|------|--------------|
| High | Split Kafka inbox claim TX from Feign/SMTP handlers (`IntegrationEventListener`) | Needs inbox status/reclaim design so retries remain safe |
| High | Distributed cache single-flight (Redis `SET NX`) for multi-pod stampede | JVM `ReentrantLock` only protects one instance |
| Medium | Paginate timeline/notes/activities/duplicate list APIs | API shape change; introduce optional `Pageable` with defaults |
| Medium | Cap Redis semantic-cache payload size | Needs product decision on truncation vs skip |
| Medium | Caffeine maxSize/TTL for `JpaPipelineTransitionSource` in-memory graph cache | Low cardinality today; still grows with tenants |
| Low | `User.roles` EAGER → LAZY + `@EntityGraph` on auth paths | Touch identity auth hot path carefully |
| Low | Outbox post-send status bulk UPDATE | Secondary to Kafka latency |

---

## Verification

Targeted tests executed successfully for:

- `common-cache` (`PlatformCacheServiceTest`, `TenantCacheKeyGeneratorTest`)
- `lead-service` (`DuplicateLeadDetectionServiceTest`)
- `ai-service` (`KnowledgeIndexingServiceTest`, `SemanticCacheServiceTest`)
- `workflow-service` (`WorkflowAutomationEngineTest`)
- `customer-service` / `conversation-service` compile after signature changes

---

## Files modified (summary)

| Module | Files |
|--------|-------|
| common-cache | `PlatformCacheService.java`, `PlatformCacheServiceTest.java` |
| ai-service | `KnowledgeChunkVectorRepository.java`, `DocumentEmbeddingPipeline.java` |
| customer-service | `CustomerDuplicateDetectionService.java`, `CustomerDuplicateRepository.java`, `CustomerRepository.java` |
| lead-service | `DuplicateLeadDetectionService.java`, `LeadRepository.java`, `DuplicateLeadDetectionServiceTest.java` |
| conversation-service | `ConversationMessageRepository.java`, `ConversationContextService.java` |
| workflow-service | `WorkflowAutomationExecutionRepository.java`, `WorkflowAutomationEngine.java` |
| docs | `docs/12-operations/platform-performance-audit-report.md` (this report) |
