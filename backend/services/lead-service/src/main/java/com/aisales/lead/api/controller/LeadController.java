package com.aisales.lead.api.controller;

import com.aisales.common.contracts.lead.ArchiveLeadRequest;
import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.AssignmentPoolMemberDto;
import com.aisales.common.contracts.lead.CancelLeadVisitRequest;
import com.aisales.common.contracts.lead.ChangeLeadStatusRequest;
import com.aisales.common.contracts.lead.ConvertLeadRequest;
import com.aisales.common.contracts.lead.CreateLeadAttachmentRequest;
import com.aisales.common.contracts.lead.CreateLeadAttributionRequest;
import com.aisales.common.contracts.lead.CreateLeadCustomFieldRequest;
import com.aisales.common.contracts.lead.CreateLeadFollowupRequest;
import com.aisales.common.contracts.lead.CreateLeadNoteRequest;
import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadActivityDto;
import com.aisales.common.contracts.lead.LeadAttachmentDto;
import com.aisales.common.contracts.lead.LeadAttributionDto;
import com.aisales.common.contracts.lead.LeadCustomFieldDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadDuplicateDto;
import com.aisales.common.contracts.lead.LeadFollowupDto;
import com.aisales.common.contracts.lead.LeadNoteDto;
import com.aisales.common.contracts.lead.LeadQualityScoreDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.LeadStatusHistoryDto;
import com.aisales.common.contracts.lead.LeadTimelineEntryDto;
import com.aisales.common.contracts.lead.LoseLeadRequest;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.contracts.lead.ScheduleLeadVisitRequest;
import com.aisales.common.contracts.lead.ScoreLeadRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.contracts.lead.UpsertAssignmentPoolMemberRequest;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.lead.application.service.LeadAiQualificationService;
import com.aisales.lead.application.service.LeadAssignmentPoolService;
import com.aisales.lead.application.service.LeadExtensionService;
import com.aisales.lead.application.service.LeadJourneyService;
import com.aisales.lead.application.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Lead lifecycle management")
public class LeadController {

    private final LeadService leadService;
    private final LeadExtensionService extensionService;
    private final LeadAssignmentPoolService assignmentPoolService;
    private final LeadJourneyService journeyService;
    private final LeadAiQualificationService aiQualificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lead")
    public ApiResponse<LeadDto> createLead(
            @Valid @RequestBody CreateLeadRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = ApiConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        return ok("Lead created", leadService.createLead(request, idempotencyKey));
    }

    @GetMapping
    @Operation(summary = "Search / list leads")
    public ApiResponse<PageResponse<LeadDto>> listLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String q) {
        return ok(leadService.listLeads(page, size, status, assignedTo, sourceType, q));
    }

    @GetMapping("/duplicates")
    @Operation(summary = "List duplicate detections")
    public ApiResponse<List<LeadDuplicateDto>> listDuplicates(
            @RequestParam(required = false) Boolean resolved) {
        return ok(leadService.listDuplicates(resolved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lead")
    public ApiResponse<LeadDto> getLead(@PathVariable UUID id) {
        return ok(leadService.getLead(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update lead details")
    public ApiResponse<LeadDto> updateLead(
            @PathVariable UUID id, @Valid @RequestBody UpdateLeadRequest request) {
        return ok("Lead updated", leadService.updateLead(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete lead")
    public void deleteLead(@PathVariable UUID id) {
        leadService.deleteLead(id);
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate lead (required before assignment)")
    public ApiResponse<LeadDto> validate(@PathVariable UUID id) {
        return ok("Lead validated", leadService.validateLead(id));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign lead to a user")
    public ApiResponse<LeadDto> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignLeadRequest request) {
        return ok("Lead assigned", leadService.assignLead(id, request));
    }

    @PostMapping("/{id}/qualify")
    @Operation(summary = "Qualify lead")
    public ApiResponse<LeadDto> qualify(
            @PathVariable UUID id, @Valid @RequestBody QualifyLeadRequest request) {
        return ok("Lead qualified", leadService.qualifyLead(id, request));
    }

    @PostMapping("/{id}/ai-qualification")
    @Operation(summary = "Qualify lead via AI Gateway (same API for all industries; prompt/vars differ)")
    public ApiResponse<AiLeadQualificationResultDto> qualifyWithAi(
            @PathVariable UUID id, @Valid @RequestBody QualifyLeadWithAiRequest request) {
        return ok("AI qualification completed", aiQualificationService.qualifyWithAi(id, request));
    }

    @PostMapping("/{id}/contact")
    @Operation(summary = "Mark lead contacted")
    public ApiResponse<LeadDto> contact(
            @PathVariable UUID id, @RequestParam(required = false) String channel) {
        return ok("Lead contacted", leadService.contactLead(id, channel));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Change lead status")
    public ApiResponse<LeadDto> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeLeadStatusRequest request) {
        return ok("Status updated", leadService.changeStatus(id, request));
    }

    @PostMapping("/{id}/score")
    @Operation(summary = "Score lead")
    public ApiResponse<LeadDto> score(
            @PathVariable UUID id, @Valid @RequestBody ScoreLeadRequest request) {
        return ok("Lead scored", leadService.scoreLead(id, request));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert lead (WON)")
    public ApiResponse<LeadDto> convert(
            @PathVariable UUID id, @RequestBody(required = false) ConvertLeadRequest request) {
        ConvertLeadRequest body = request != null ? request : new ConvertLeadRequest();
        return ok("Lead converted", leadService.convertLead(id, body));
    }

    @PostMapping("/{id}/lose")
    @Operation(summary = "Mark lead lost")
    public ApiResponse<LeadDto> lose(
            @PathVariable UUID id, @Valid @RequestBody LoseLeadRequest request) {
        return ok("Lead lost", leadService.loseLead(id, request));
    }

    @PostMapping("/{id}/schedule-visit")
    @Operation(summary = "Schedule site visit (journey command)")
    public ApiResponse<LeadDto> scheduleVisit(
            @PathVariable UUID id, @RequestBody(required = false) ScheduleLeadVisitRequest request) {
        ScheduleLeadVisitRequest body = request != null ? request : new ScheduleLeadVisitRequest();
        return ok("Visit scheduled", journeyService.scheduleVisit(id, body));
    }

    @PostMapping("/{id}/complete-visit")
    @Operation(summary = "Complete site visit (journey command)")
    public ApiResponse<LeadDto> completeVisit(
            @PathVariable UUID id, @RequestParam(required = false) String notes) {
        return ok("Visit completed", journeyService.completeVisit(id, notes));
    }

    @PostMapping("/{id}/cancel-visit")
    @Operation(summary = "Cancel scheduled visit (journey command)")
    public ApiResponse<LeadDto> cancelVisit(
            @PathVariable UUID id, @RequestBody(required = false) CancelLeadVisitRequest request) {
        CancelLeadVisitRequest body = request != null ? request : new CancelLeadVisitRequest();
        return ok("Visit cancelled", journeyService.cancelVisit(id, body));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive closed lead (WON/LOST → ARCHIVED)")
    public ApiResponse<LeadDto> archive(
            @PathVariable UUID id, @RequestBody(required = false) ArchiveLeadRequest request) {
        ArchiveLeadRequest body = request != null ? request : new ArchiveLeadRequest();
        return ok("Lead archived", journeyService.archiveLead(id, body));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Append-only lead journey timeline")
    public ApiResponse<List<LeadTimelineEntryDto>> timeline(@PathVariable UUID id) {
        return ok(journeyService.timeline(id));
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add note")
    public ApiResponse<LeadNoteDto> addNote(
            @PathVariable UUID id, @Valid @RequestBody CreateLeadNoteRequest request) {
        return ok("Note added", leadService.addNote(id, request));
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "List notes")
    public ApiResponse<List<LeadNoteDto>> listNotes(@PathVariable UUID id) {
        return ok(leadService.listNotes(id));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "List activities")
    public ApiResponse<List<LeadActivityDto>> listActivities(@PathVariable UUID id) {
        return ok(leadService.listActivities(id));
    }

    @PostMapping("/{id}/followups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule follow-up")
    public ApiResponse<LeadFollowupDto> scheduleFollowup(
            @PathVariable UUID id, @Valid @RequestBody CreateLeadFollowupRequest request) {
        return ok("Follow-up scheduled", leadService.scheduleFollowup(id, request));
    }

    @GetMapping("/{id}/followups")
    @Operation(summary = "List follow-ups")
    public ApiResponse<List<LeadFollowupDto>> listFollowups(@PathVariable UUID id) {
        return ok(leadService.listFollowups(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Status history")
    public ApiResponse<List<LeadStatusHistoryDto>> history(@PathVariable UUID id) {
        return ok(leadService.listHistory(id));
    }

    @PostMapping("/{id}/duplicates/{duplicateId}/resolve")
    @Operation(summary = "Resolve duplicate detection")
    public ApiResponse<LeadDuplicateDto> resolveDuplicate(
            @PathVariable UUID id,
            @PathVariable UUID duplicateId,
            @RequestParam(required = false) UUID mergeIntoLeadId) {
        return ok("Duplicate resolved", leadService.resolveDuplicate(id, duplicateId, mergeIntoLeadId));
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add attachment metadata (binary via media-service)")
    public ApiResponse<LeadAttachmentDto> addAttachment(
            @PathVariable UUID id, @Valid @RequestBody CreateLeadAttachmentRequest request) {
        return ok("Attachment added", extensionService.addAttachment(id, request));
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "List attachments")
    public ApiResponse<List<LeadAttachmentDto>> listAttachments(@PathVariable UUID id) {
        return ok(extensionService.listAttachments(id));
    }

    @PostMapping("/custom-fields")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Define tenant custom field")
    public ApiResponse<LeadCustomFieldDto> createCustomField(
            @Valid @RequestBody CreateLeadCustomFieldRequest request) {
        return ok("Custom field created", extensionService.createCustomField(request));
    }

    @GetMapping("/custom-fields")
    @Operation(summary = "List tenant custom fields")
    public ApiResponse<List<LeadCustomFieldDto>> listCustomFields() {
        return ok(extensionService.listCustomFields());
    }

    @PostMapping("/{id}/attributions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record marketing attribution")
    public ApiResponse<LeadAttributionDto> addAttribution(
            @PathVariable UUID id, @Valid @RequestBody CreateLeadAttributionRequest request) {
        return ok("Attribution added", extensionService.addAttribution(id, request));
    }

    @GetMapping("/{id}/attributions")
    @Operation(summary = "List attributions")
    public ApiResponse<List<LeadAttributionDto>> listAttributions(@PathVariable UUID id) {
        return ok(extensionService.listAttributions(id));
    }

    @PostMapping("/{id}/quality-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record AI quality score (validated by lead-service)")
    public ApiResponse<LeadQualityScoreDto> recordQualityScore(
            @PathVariable UUID id, @Valid @RequestBody RecordLeadQualityScoreRequest request) {
        return ok("Quality score recorded", extensionService.recordQualityScore(id, request));
    }

    @GetMapping("/{id}/quality-scores")
    @Operation(summary = "List quality scores")
    public ApiResponse<List<LeadQualityScoreDto>> listQualityScores(@PathVariable UUID id) {
        return ok(extensionService.listQualityScores(id));
    }

    @PutMapping("/assignment-pool")
    @Operation(summary = "Upsert round-robin assignment pool member")
    public ApiResponse<AssignmentPoolMemberDto> upsertPoolMember(
            @Valid @RequestBody UpsertAssignmentPoolMemberRequest request) {
        return ok("Pool member upserted", assignmentPoolService.upsert(request));
    }

    @GetMapping("/assignment-pool")
    @Operation(summary = "List assignment pool members")
    public ApiResponse<List<AssignmentPoolMemberDto>> listPoolMembers() {
        return ok(assignmentPoolService.list());
    }

    private <T> ApiResponse<T> ok(T data) {
        return withCorrelation(ApiResponse.ok(data));
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return withCorrelation(ApiResponse.ok(message, data));
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
