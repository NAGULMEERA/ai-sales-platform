# ADR-024: AI Gateway

**Label:** ADR-005 (AI Gateway)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Business services must not call LLM vendors directly. Prompts must be versioned. AI output is untrusted until Business Services validate it.

## Decision

1. **ai-service** is the AI Gateway: prompt registry, execute API, embeddings, KB metadata.
2. Execution path: resolve prompt version → render variables → `LlmProvider` port → structured output + confidence.
3. Default provider may be STUB; real vendors stay behind the port **inside ai-service only**.
4. Plugins **reference prompt IDs/codes**; they do not embed production prompt strings long-term.
5. Prompt Registry evolves toward Industry + Language + Version + Model dimensions — without moving business decisions into AI.
6. Knowledge Registry (industry/category/documents/embeddings) is consumed by AI Gateway later; full RAG ingest is not required for freeze.

## Consequences

- Lead/Deal/Workflow never depend on OpenAI/Gemini SDKs.
- Vertical validation Sprint (Qualification): same `/api/v1/ai/execute` for Real Estate and Automobile; only prompt variables/metadata differ.
- AI never updates aggregates directly.
