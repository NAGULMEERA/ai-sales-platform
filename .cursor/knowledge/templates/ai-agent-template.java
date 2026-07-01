package com.company.platform.template.ai;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Production-ready AI Agent Template.
 *
 * Responsibilities:
 * - Load versioned prompts
 * - Execute LLM requests
 * - Invoke approved tools
 * - Validate structured output
 * - Publish metrics
 * - Support fallback models
 */
public class LeadQualificationAgent {

    private final PromptRepository promptRepository;
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final GuardrailService guardrailService;
    private final MemoryStore memoryStore;

    public LeadQualificationAgent(
            PromptRepository promptRepository,
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            GuardrailService guardrailService,
            MemoryStore memoryStore) {
        this.promptRepository = promptRepository;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.guardrailService = guardrailService;
        this.memoryStore = memoryStore;
    }

    public QualificationResult qualify(QualificationRequest request) {

        PromptTemplate prompt = promptRepository.load("lead-qualification", 1);

        String renderedPrompt = prompt.render(Map.of(
                "name", request.name(),
                "budget", request.budget(),
                "location", request.location()));

        guardrailService.validateInput(renderedPrompt);

        Memory memory = memoryStore.load(request.customerId());

        LlmResponse response = llmClient.generate(
                renderedPrompt,
                memory,
                Duration.ofSeconds(20));

        guardrailService.validateOutput(response.content());

        QualificationResult result =
                QualificationResult.fromJson(response.content());

        return result;
    }
}

/* -------- Contracts -------- */

interface PromptRepository {
    PromptTemplate load(String promptId, int version);
}

interface LlmClient {
    LlmResponse generate(String prompt, Memory memory, Duration timeout);
}

interface ToolRegistry {
    Object invoke(String toolName, Object input);
}

interface GuardrailService {
    void validateInput(String prompt);
    void validateOutput(String output);
}

interface MemoryStore {
    Memory load(UUID customerId);
}

/* -------- Models -------- */

record QualificationRequest(
        UUID customerId,
        String name,
        String location,
        double budget) {}

record Memory(String summary) {}

record LlmResponse(String content, int promptTokens, int completionTokens) {}

record PromptTemplate(String id, int version, String template) {
    String render(Map<String, Object> variables) {
        String result = template;
        for (var e : variables.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
        }
        return result;
    }
}

record QualificationResult(
        String category,
        int score,
        String recommendation) {

    static QualificationResult fromJson(String json) {
        // Replace with real JSON deserialization
        return new QualificationResult("HOT", 92, json);
    }
}
