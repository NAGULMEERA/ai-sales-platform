package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecuteRequest {

    /**
     * Prompt code (preferred when known). Optional when industry + capability dimensions are set.
     */
    @Size(max = 100)
    private String promptCode;

    /** Optional explicit version; defaults to active version. */
    private Integer promptVersion;

    private UUID promptId;

    /** Dimension resolution: industry (e.g. REAL_ESTATE). */
    @Size(max = 64)
    private String industryCode;

    /** Dimension resolution: language (e.g. en). */
    @Size(max = 16)
    private String languageCode;

    /** Dimension resolution: capability (e.g. LEAD_QUALIFICATION). */
    @Size(max = 64)
    private String capability;

    /** Optional preferred model hint forwarded with resolved prompt metadata. */
    @Size(max = 128)
    private String preferredModel;

    @Builder.Default
    private Map<String, String> variables = new HashMap<>();

    /** Optional correlation for business callers (leadId, etc.). Not stored as FK. */
    private String businessReference;

    /**
     * When set, AI Gateway retrieves tenant-scoped chunks from this knowledge base
     * and assembles them into the prompt context (RAG).
     */
    private UUID knowledgeBaseId;

    /**
     * Optional retrieval query. Defaults to {@code variables.question}, then
     * {@code variables.query}, then a concatenation of variable values.
     */
    private String retrievalQuery;

    /** Max chunks to retrieve (default applied in AI Gateway). */
    private Integer retrievalTopK;

    /**
     * When true, response includes rendered system/user prompts (debug/admin only).
     * Defaults to false so clients do not receive full prompt text or retrieved knowledge.
     */
    @Builder.Default
    private boolean includeRenderedPrompts = false;
}
