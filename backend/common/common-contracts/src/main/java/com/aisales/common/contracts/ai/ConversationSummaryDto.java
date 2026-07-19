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
public class ConversationSummaryDto {

    private UUID executionId;
    private String summary;
    private String sentiment;
    private String intent;

    @Builder.Default
    private List<String> keyTopics = new ArrayList<>();

    @Builder.Default
    private List<String> openQuestions = new ArrayList<>();
}
