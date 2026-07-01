package com.company.platform.template.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Production-ready REST Controller Template.
 *
 * Responsibilities:
 * - Validate requests
 * - Delegate to Application Service
 * - Return DTOs
 * - No business logic
 */
@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadApplicationService service;

    public LeadController(LeadApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateLeadRequest request) {

        UUID id = service.createLead(
                new CreateLeadCommand(
                        request.name(),
                        request.email(),
                        request.phone()));

        return ResponseEntity
                .created(URI.create("/api/v1/leads/" + id))
                .body(new LeadResponse(id, "CAPTURED"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getLead(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getLead(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLead(@PathVariable UUID id) {
        service.deleteLead(id);
    }
}

/* ---------- Request DTO ---------- */

record CreateLeadRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String phone) {}

/* ---------- Response DTO ---------- */

record LeadResponse(
        UUID id,
        String status) {}

/* ---------- Command ---------- */

record CreateLeadCommand(
        String name,
        String email,
        String phone) {}

/* ---------- Application Port ---------- */

interface LeadApplicationService {

    UUID createLead(CreateLeadCommand command);

    LeadResponse getLead(UUID id);

    void deleteLead(UUID id);
}
