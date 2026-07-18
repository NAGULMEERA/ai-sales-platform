package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.AssignmentPoolMemberDto;
import com.aisales.common.contracts.lead.UpsertAssignmentPoolMemberRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.LeadAssignmentPoolMember;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentPoolRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LeadAssignmentPoolService {

    private final LeadAssignmentPoolRepository poolRepository;
    private final LeadMapper leadMapper;

    @Transactional
    public AssignmentPoolMemberDto upsert(UpsertAssignmentPoolMemberRequest request) {
        UUID tenantId = requireTenantId();
        Instant now = Instant.now();
        LeadAssignmentPoolMember member = poolRepository
                .findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseGet(() -> LeadAssignmentPoolMember.builder()
                        .tenantId(tenantId)
                        .userId(request.getUserId())
                        .enabled(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
        if (request.getEnabled() != null) {
            member.setEnabled(request.getEnabled());
        }
        member.setUpdatedAt(now);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now);
        }
        return leadMapper.toPoolMemberDto(poolRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<AssignmentPoolMemberDto> list() {
        return poolRepository.findByTenantIdOrderByCreatedAtAsc(requireTenantId()).stream()
                .map(leadMapper::toPoolMemberDto).toList();
    }

    @Transactional
    public UUID nextRoundRobinAssignee(UUID tenantId) {
        List<LeadAssignmentPoolMember> enabled = poolRepository.findEnabledForRoundRobin(tenantId);
        if (enabled.isEmpty()) {
            throw new ValidationException("No enabled assignees in the round-robin pool");
        }
        LeadAssignmentPoolMember selected = enabled.getFirst();
        selected.setLastAssignedAt(Instant.now());
        selected.setUpdatedAt(Instant.now());
        poolRepository.save(selected);
        return selected.getUserId();
    }

    private UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(tenantId);
    }
}
