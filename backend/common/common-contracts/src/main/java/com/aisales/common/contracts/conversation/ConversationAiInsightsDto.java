package com.aisales.common.contracts.conversation;

import com.aisales.common.contracts.ai.ConversationSummaryDto;
import com.aisales.common.contracts.ai.FollowUpSuggestionDto;
import com.aisales.common.contracts.ai.SentimentAnalysisDto;
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
public class ConversationAiInsightsDto {

    private UUID conversationId;
    private UUID executionId;
    private ConversationSummaryDto summary;
    private SentimentAnalysisDto sentiment;
    private String intent;
    private String classification;
    private String nextBestAction;
    private String replySuggestion;
    private FollowUpSuggestionDto followUp;
    private Double confidence;

    @Builder.Default
    private List<String> knowledgeSnippets = new ArrayList<>();
}
