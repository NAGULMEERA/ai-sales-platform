package com.aisales.conversation.application.service;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.ConversationSummaryDto;
import com.aisales.common.contracts.ai.FollowUpSuggestionDto;
import com.aisales.common.contracts.ai.SentimentAnalysisDto;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.ConversationAiInsightsDto;
import com.aisales.common.contracts.conversation.ConversationContextDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.MessageDirection;
import com.aisales.common.contracts.conversation.MessageSenderType;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.AiReplyGeneratedEvent;
import com.aisales.common.events.model.ConversationSummarizedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Conversation AI orchestration. Calls AI Gateway only — never provider SDKs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAiService {

    private static final String CAPABILITY = "CONVERSATION_ASSIST";
    private static final String PROMPT_CODE = "CONVERSATION_INSIGHTS_V1";

    private final ConversationService conversationService;
    private final ConversationContextService contextService;
    private final AiServiceClient aiServiceClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public ConversationAiInsightsDto generateInsights(UUID conversationId) {
        ConversationContextDto context = contextService.loadContext(conversationId);
        ConversationThread thread = conversationService.requireThread(conversationId);
        AiExecuteResponse executed = executeGateway(context);

        ConversationAiInsightsDto insights = parseInsights(conversationId, executed);
        conversationService.applyAiInsights(
                conversationId,
                insights.getSummary() == null ? null : insights.getSummary().getSummary(),
                insights.getSentiment() == null ? null : insights.getSentiment().getSentiment(),
                insights.getIntent(),
                insights.getClassification(),
                insights.getNextBestAction());

        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        String leadId = thread.getLeadId() == null ? null : thread.getLeadId().toString();
        String executionId = executed.getExecutionId() == null ? null : executed.getExecutionId().toString();
        String confidence = executed.getConfidence() == null ? null : executed.getConfidence().toString();

        eventPublisher.publish(ConversationSummarizedEvent.of(
                thread.getTenantId().toString(),
                conversationId.toString(),
                leadId,
                executionId,
                insights.getSentiment() == null ? null : insights.getSentiment().getSentiment(),
                insights.getIntent(),
                correlationId));
        eventPublisher.publish(AiReplyGeneratedEvent.of(
                thread.getTenantId().toString(),
                conversationId.toString(),
                leadId,
                executionId,
                insights.getIntent(),
                confidence,
                insights.getNextBestAction(),
                correlationId));

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementForTenant(
                    MetricNames.CONVERSATION_AI_INSIGHT, thread.getTenantId().toString());
        }
        return insights;
    }

    @Transactional
    public ConversationMessageDto suggestAndPostReply(UUID conversationId) {
        ConversationAiInsightsDto insights = generateInsights(conversationId);
        if (!StringUtils.hasText(insights.getReplySuggestion())) {
            throw new ValidationException("AI did not produce a reply suggestion");
        }
        return conversationService.addMessage(conversationId, AddMessageRequest.builder()
                .senderType(MessageSenderType.AI)
                .body(insights.getReplySuggestion().trim())
                .direction(MessageDirection.OUTBOUND)
                .build());
    }

    private AiExecuteResponse executeGateway(ConversationContextDto context) {
        Map<String, String> variables = new HashMap<>();
        variables.put("conversationId", context.getConversationId().toString());
        variables.put("channel", context.getChannel() == null ? "WEB" : context.getChannel().name());
        variables.put("messages", formatMessages(context.getPreviousMessages()));
        if (context.getLead() != null) {
            variables.put("leadName", nullToEmpty(context.getLead().getCustomerName()));
            variables.put(
                    "leadScore",
                    context.getLead().getScore() == null ? "" : context.getLead().getScore().toString());
            variables.put("leadStatus", context.getLead().getStatus() == null
                    ? ""
                    : context.getLead().getStatus().name());
        }
        if (context.getCustomer() != null) {
            variables.put("customerName", nullToEmpty(context.getCustomer().getFullName()));
        }
        if (context.getOpportunity() != null) {
            variables.put(
                    "opportunityStatus",
                    context.getOpportunity().getStatus() == null
                            ? ""
                            : context.getOpportunity().getStatus().name());
            variables.put(
                    "opportunityName", nullToEmpty(context.getOpportunity().getName()));
        }
        if (context.getCatalogRecommendation() != null
                && context.getCatalogRecommendation().getRecommendations() != null
                && !context.getCatalogRecommendation().getRecommendations().isEmpty()) {
            var primary = context.getCatalogRecommendation().getRecommendations().getFirst();
            variables.put(
                    "catalogPrimaryProductId",
                    primary.getProductId() == null ? "" : primary.getProductId().toString());
            variables.put("catalogPrimaryScore", String.valueOf(primary.getMatchScore()));
        }
        try {
            variables.put("metadata", objectMapper.writeValueAsString(context.getMetadata()));
        } catch (Exception ex) {
            variables.put("metadata", "{}");
        }

        ApiResponse<AiExecuteResponse> response = aiServiceClient.execute(AiExecuteRequest.builder()
                .promptCode(PROMPT_CODE)
                .capability(CAPABILITY)
                .variables(variables)
                .businessReference(context.getConversationId().toString())
                .retrievalQuery(buildRetrievalQuery(context))
                .build());
        if (response == null || response.getData() == null) {
            throw new ValidationException("AI Gateway returned empty conversation insights");
        }
        return response.getData();
    }

    private ConversationAiInsightsDto parseInsights(UUID conversationId, AiExecuteResponse executed) {
        Map<String, Object> structured =
                executed.getStructuredOutput() == null ? Map.of() : executed.getStructuredOutput();

        String summaryText = stringVal(structured.get("summary"), executed.getRawText());
        String sentiment = stringVal(structured.get("sentiment"), "NEUTRAL");
        String intent = stringVal(structured.get("intent"), "GENERAL");
        String classification = stringVal(structured.get("classification"), "SALES");
        String nextBestAction = stringVal(structured.get("nextBestAction"), "FOLLOW_UP");
        String replySuggestion = stringVal(structured.get("replySuggestion"), summaryText);
        Double sentimentScore = doubleVal(structured.get("sentimentScore"), executed.getConfidence());

        List<String> knowledge = executed.getRetrievedChunks() == null
                ? List.of()
                : executed.getRetrievedChunks().stream()
                        .map(chunk -> chunk.getContent())
                        .filter(StringUtils::hasText)
                        .limit(5)
                        .toList();

        return ConversationAiInsightsDto.builder()
                .conversationId(conversationId)
                .executionId(executed.getExecutionId())
                .summary(ConversationSummaryDto.builder()
                        .executionId(executed.getExecutionId())
                        .summary(summaryText)
                        .sentiment(sentiment)
                        .intent(intent)
                        .build())
                .sentiment(SentimentAnalysisDto.builder()
                        .executionId(executed.getExecutionId())
                        .sentiment(sentiment)
                        .score(sentimentScore)
                        .rationale(stringVal(structured.get("sentimentRationale"), null))
                        .build())
                .intent(intent)
                .classification(classification)
                .nextBestAction(nextBestAction)
                .replySuggestion(replySuggestion)
                .followUp(FollowUpSuggestionDto.builder()
                        .executionId(executed.getExecutionId())
                        .channel(stringVal(structured.get("followUpChannel"), "WHATSAPP"))
                        .messageDraft(stringVal(structured.get("followUpDraft"), replySuggestion))
                        .suggestedTiming(stringVal(structured.get("followUpTiming"), "WITHIN_24H"))
                        .build())
                .confidence(executed.getConfidence())
                .knowledgeSnippets(knowledge)
                .build();
    }

    private static String formatMessages(List<ConversationMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.stream()
                .map(m -> m.getSenderType() + ": " + m.getBody())
                .collect(Collectors.joining("\n"));
    }

    private static String buildRetrievalQuery(ConversationContextDto context) {
        if (context.getPreviousMessages() == null || context.getPreviousMessages().isEmpty()) {
            return "conversation assistance";
        }
        ConversationMessageDto last = context.getPreviousMessages().getLast();
        return last.getBody();
    }

    private static String stringVal(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private static Double doubleVal(Object value, Double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
