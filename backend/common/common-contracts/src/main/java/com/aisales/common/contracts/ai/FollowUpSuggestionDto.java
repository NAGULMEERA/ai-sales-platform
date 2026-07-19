package com.aisales.common.contracts.ai;

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
public class FollowUpSuggestionDto {

    private UUID executionId;
    private String channel;
    private String messageDraft;
    private String suggestedTiming;

    @Builder.Default
    private List<String> questions = new ArrayList<>();
}
