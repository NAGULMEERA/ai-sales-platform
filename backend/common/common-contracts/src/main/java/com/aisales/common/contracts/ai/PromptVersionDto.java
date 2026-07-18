package com.aisales.common.contracts.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionDto {

    private UUID id;
    private UUID promptId;
    private Integer versionNumber;
    private String systemTemplate;
    private String userTemplate;
    @Builder.Default
    private List<String> variables = new ArrayList<>();
    private String expectedOutputHint;
    private String changelog;
    private PromptStatus status;
    private Instant createdAt;
}
