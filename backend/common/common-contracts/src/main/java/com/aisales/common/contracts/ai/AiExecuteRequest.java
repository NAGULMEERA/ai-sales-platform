package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.NotBlank;
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

    /** Prompt code (preferred). */
    @NotBlank
    private String promptCode;

    /** Optional explicit version; defaults to active version. */
    private Integer promptVersion;

    private UUID promptId;

    @Builder.Default
    private Map<String, String> variables = new HashMap<>();

    /** Optional correlation for business callers (leadId, etc.). Not stored as FK. */
    private String businessReference;
}
