package com.aisales.conversation.api.controller;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.AddParticipantRequest;
import com.aisales.common.contracts.conversation.ConversationAiInsightsDto;
import com.aisales.common.contracts.conversation.ConversationContextDto;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.ConversationParticipantDto;
import com.aisales.common.contracts.conversation.ConversationTimelineEntryDto;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
import com.aisales.common.contracts.conversation.UpdateConversationMetadataRequest;
import com.aisales.common.contracts.conversation.UpdateMessageStatusRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.conversation.application.service.ConversationAiService;
import com.aisales.conversation.application.service.ConversationContextService;
import com.aisales.conversation.application.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Conversations", description = "Lead/customer conversation threads and messages")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationContextService conversationContextService;
    private final ConversationAiService conversationAiService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a conversation")
    public ApiResponse<ConversationDto> start(@Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.ok(conversationService.start(request));
    }

    @GetMapping
    @Operation(summary = "List conversations (optionally by leadId)")
    public ApiResponse<PageResponse<ConversationDto>> list(
            @RequestParam(required = false) UUID leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(conversationService.list(leadId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation")
    public ApiResponse<ConversationDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.get(id));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a message")
    public ApiResponse<ConversationMessageDto> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddMessageRequest request) {
        return ApiResponse.ok(conversationService.addMessage(id, request));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "List messages")
    public ApiResponse<List<ConversationMessageDto>> listMessages(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.listMessages(id));
    }

    @PutMapping("/{id}/messages/{messageId}/status")
    @Operation(summary = "Update message delivery status")
    public ApiResponse<ConversationMessageDto> updateMessageStatus(
            @PathVariable UUID id,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageStatusRequest request) {
        return ApiResponse.ok(conversationService.updateMessageStatus(id, messageId, request));
    }

    @PostMapping("/{id}/messages/{messageId}/retry")
    @Operation(summary = "Retry a failed outbound message")
    public ApiResponse<ConversationMessageDto> retryMessage(
            @PathVariable UUID id, @PathVariable UUID messageId) {
        return ApiResponse.ok(conversationService.retryMessage(id, messageId));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a conversation")
    public ApiResponse<ConversationDto> close(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(conversationService.close(id, reason));
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Replace conversation metadata")
    public ApiResponse<ConversationDto> updateMetadata(
            @PathVariable UUID id, @Valid @RequestBody UpdateConversationMetadataRequest request) {
        return ApiResponse.ok(conversationService.updateMetadata(id, request));
    }

    @PostMapping("/{id}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a participant")
    public ApiResponse<ConversationParticipantDto> addParticipant(
            @PathVariable UUID id, @Valid @RequestBody AddParticipantRequest request) {
        return ApiResponse.ok(conversationService.addParticipant(id, request));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "List participants")
    public ApiResponse<List<ConversationParticipantDto>> listParticipants(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.listParticipants(id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Conversation timeline")
    public ApiResponse<List<ConversationTimelineEntryDto>> timeline(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.timeline(id));
    }

    @GetMapping("/{id}/context")
    @Operation(summary = "Load conversation context for AI Gateway")
    public ApiResponse<ConversationContextDto> context(@PathVariable UUID id) {
        return ApiResponse.ok(conversationContextService.loadContext(id));
    }

    @PostMapping("/{id}/ai/insights")
    @Operation(summary = "Generate AI conversation insights via AI Gateway")
    public ApiResponse<ConversationAiInsightsDto> aiInsights(@PathVariable UUID id) {
        return ApiResponse.ok(conversationAiService.generateInsights(id));
    }

    @PostMapping("/{id}/ai/reply")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate AI reply suggestion and post as AI message")
    public ApiResponse<ConversationMessageDto> aiReply(@PathVariable UUID id) {
        return ApiResponse.ok(conversationAiService.suggestAndPostReply(id));
    }
}
