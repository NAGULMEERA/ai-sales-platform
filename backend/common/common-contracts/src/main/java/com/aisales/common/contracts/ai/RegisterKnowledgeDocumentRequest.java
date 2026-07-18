package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.NotBlank;
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
public class RegisterKnowledgeDocumentRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String contentType;

    private Long sizeBytes;

    private UUID mediaId;

    @Size(max = 1000)
    private String objectKey;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
