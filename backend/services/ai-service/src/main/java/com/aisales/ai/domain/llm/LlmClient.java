package com.aisales.ai.domain.llm;

/**
 * Pluggable LLM backend (STUB, OPENAI, …). {@link LlmProvider} routes to one of these.
 */
public interface LlmClient {

    /** Stable provider key, e.g. {@code STUB}, {@code OPENAI}. */
    String name();

    LlmCompletionResult complete(LlmCompletionRequest request);
}
