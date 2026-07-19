package com.aisales.ai.api.controller;

import com.aisales.ai.application.service.AiGatewayService;
import com.aisales.ai.application.service.AiQualificationOrchestrator;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.QualificationResultDto;
import com.aisales.common.contracts.ai.QualifyLeadAiRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "AI Gateway", description = "Provider-abstracted prompt execution and qualification")
public class AiGatewayController {

    private final AiGatewayService aiGatewayService;
    private final AiQualificationOrchestrator qualificationOrchestrator;

    @PostMapping("/execute")
    @Operation(summary = "Execute a versioned prompt via the AI gateway (business services validate output)")
    public ApiResponse<AiExecuteResponse> execute(@Valid @RequestBody AiExecuteRequest request) {
        return ApiResponse.ok(aiGatewayService.execute(request));
    }

    @PostMapping("/qualify")
    @Operation(summary = "Lead qualification orchestration (structured output via AI Gateway)")
    public ApiResponse<QualificationResultDto> qualify(@Valid @RequestBody QualifyLeadAiRequest request) {
        return ApiResponse.ok(qualificationOrchestrator.qualify(request));
    }
}
