package com.aisales.identity.application.service;

import com.aisales.identity.infrastructure.persistence.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RbacService rbacService;

    @Test
    void shouldResolvePermissionsForRoles() {
        when(permissionRepository.findPermissionCodesByRoleNames(Set.of("USER")))
                .thenReturn(Set.of("lead:read", "lead:create"));

        Set<String> permissions = rbacService.resolvePermissions(Set.of("USER"));

        assertThat(permissions).containsExactlyInAnyOrder("lead:read", "lead:create");
    }

    @Test
    void shouldReturnEmptyWhenNoRoles() {
        assertThat(rbacService.resolvePermissions(Set.of())).isEmpty();
    }
}
