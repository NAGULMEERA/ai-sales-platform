package com.aisales.ai.domain.llm;

/**
 * Provider-agnostic LLM port. Business services never call vendors;
 * only ai-service implementations may talk to OpenAI/Gemini/etc.
 */
public interface LlmProvider {

    String name();

    LlmCompletionResult complete(LlmCompletionRequest request);
}
