package com.aisales.lead.api.controller;

import com.aisales.common.contracts.lead.EnsurePipelineRequest;
import com.aisales.common.contracts.lead.PipelineDto;
import com.aisales.lead.application.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipelines", description = "Tenant sales pipeline definitions (stages + transitions)")
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping
    @Operation(summary = "List active pipelines for the current tenant")
    public ResponseEntity<List<PipelineDto>> listPipelines() {
        return ResponseEntity.ok(pipelineService.listPipelines());
    }

    @GetMapping("/default")
    @Operation(summary = "Get or create the default sales pipeline (DEFAULT_SALES_V1)")
    public ResponseEntity<PipelineDto> getDefaultPipeline() {
        return ResponseEntity.ok(pipelineService.getOrCreateDefaultPipeline());
    }

    @PostMapping("/ensure")
    @Operation(summary = "Ensure a pipeline from a template code (same API for all industries)")
    public ResponseEntity<PipelineDto> ensurePipeline(@Valid @RequestBody EnsurePipelineRequest request) {
        return ResponseEntity.ok(pipelineService.ensurePipeline(request));
    }

    @GetMapping("/{pipelineId}")
    @Operation(summary = "Get a pipeline by id")
    public ResponseEntity<PipelineDto> getPipeline(@PathVariable UUID pipelineId) {
        return ResponseEntity.ok(pipelineService.getPipeline(pipelineId));
    }
}
