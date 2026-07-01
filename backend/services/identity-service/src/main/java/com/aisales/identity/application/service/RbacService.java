package com.aisales.identity.application.service;

import com.aisales.identity.infrastructure.persistence.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return permissionRepository.findPermissionCodesByRoleNames(roles);
    }
}
