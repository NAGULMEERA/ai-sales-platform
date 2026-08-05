package com.aisales.tenant.application.service;

import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.tenant.api.request.UserRequest;
import com.aisales.tenant.api.response.UserResponse;
import com.aisales.tenant.application.mapper.UserMapper;
import com.aisales.tenant.application.security.TenantAuthorization;
import com.aisales.tenant.domain.entity.User;
import com.aisales.tenant.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TenantAuthorization tenantAuthorization;

    @Transactional
    public UserResponse create(UserRequest request) {
        // The body-supplied tenantId is never trusted directly: requireTenantAccess rejects any
        // caller who is not a SUPER_ADMIN or the TENANT_ADMIN of that exact tenant, preventing a
        // caller authenticated for one tenant from creating a user record under another tenant.
        tenantAuthorization.requireTenantAccess(request.getTenantId());
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        // Platform admins may look up any user; everyone else is scoped to their own tenant to
        // prevent cross-tenant enumeration of user records by guessing/leaking a UUID.
        if (tenantAuthorization.isSuperAdmin()) {
            return userRepository.findById(id)
                    .map(userMapper::toResponse)
                    .orElseThrow(() -> new NotFoundException("User not found: " + id));
        }
        return userRepository.findByIdAndTenantId(id, requireTenantId())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listByTenant(UUID tenantId) {
        tenantAuthorization.requireTenantAccess(tenantId);
        return userRepository.findByTenantId(tenantId).stream().map(userMapper::toResponse).toList();
    }

    private UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Tenant context is required");
        }
        return UUID.fromString(tenantId);
    }
}
