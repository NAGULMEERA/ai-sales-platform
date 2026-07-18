package com.aisales.common.contracts.client;

import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.ChangeLeadStatusRequest;
import com.aisales.common.contracts.lead.ConvertLeadRequest;
import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.LoseLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.ScoreLeadRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lead-service", path = "/api/v1/leads")
public interface LeadServiceClient {

    @PostMapping
    ApiResponse<LeadDto> createLead(@RequestBody CreateLeadRequest request);

    @GetMapping("/{id}")
    ApiResponse<LeadDto> getLead(@PathVariable UUID id);

    @GetMapping
    ApiResponse<PageResponse<LeadDto>> searchLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String q);

    @PatchMapping("/{id}")
    ApiResponse<LeadDto> updateLead(@PathVariable UUID id, @RequestBody UpdateLeadRequest request);

    @DeleteMapping("/{id}")
    void deleteLead(@PathVariable UUID id);

    @PostMapping("/{id}/validate")
    ApiResponse<LeadDto> validateLead(@PathVariable UUID id);

    @PostMapping("/{id}/assign")
    ApiResponse<LeadDto> assignLead(@PathVariable UUID id, @RequestBody AssignLeadRequest request);

    @PostMapping("/{id}/qualify")
    ApiResponse<LeadDto> qualifyLead(@PathVariable UUID id, @RequestBody QualifyLeadRequest request);

    @PostMapping("/{id}/contact")
    ApiResponse<LeadDto> contactLead(@PathVariable UUID id, @RequestParam(required = false) String channel);

    @PostMapping("/{id}/status")
    ApiResponse<LeadDto> changeStatus(@PathVariable UUID id, @RequestBody ChangeLeadStatusRequest request);

    @PostMapping("/{id}/score")
    ApiResponse<LeadDto> scoreLead(@PathVariable UUID id, @RequestBody ScoreLeadRequest request);

    @PostMapping("/{id}/convert")
    ApiResponse<LeadDto> convertLead(@PathVariable UUID id, @RequestBody ConvertLeadRequest request);

    @PostMapping("/{id}/lose")
    ApiResponse<LeadDto> loseLead(@PathVariable UUID id, @RequestBody LoseLeadRequest request);
}
