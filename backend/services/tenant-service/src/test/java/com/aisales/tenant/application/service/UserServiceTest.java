package com.aisales.tenant.application.service;

import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.security.model.UserPrincipal;
import com.aisales.tenant.api.request.UserRequest;
import com.aisales.tenant.api.response.UserResponse;
import com.aisales.tenant.application.mapper.UserMapper;
import com.aisales.tenant.application.security.TenantAuthorization;
import com.aisales.tenant.domain.entity.User;
import com.aisales.tenant.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies tenant isolation for the tenant-user directory: callers may only create/read
 * users within their own tenant unless they hold SUPER_ADMIN.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    private UserService userService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper(), new TenantAuthorization());
        tenantId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void shouldRejectCreateForAnotherTenantFromTenantAdmin() {
        authenticate(tenantId, Set.of("TENANT_ADMIN"));
        UserRequest request = new UserRequest();
        request.setTenantId(UUID.randomUUID());
        request.setEmail("outsider@example.com");
        request.setFirstName("Out");
        request.setLastName("Sider");

        assertThatThrownBy(() -> userService.create(request)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldAllowCreateForOwnTenant() {
        authenticate(tenantId, Set.of("TENANT_ADMIN"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest();
        request.setTenantId(tenantId);
        request.setEmail("teammate@example.com");
        request.setFirstName("Team");
        request.setLastName("Mate");

        UserResponse response = userService.create(request);

        assertThat(response.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldAllowSuperAdminToCreateForAnyTenant() {
        authenticate(UUID.randomUUID(), Set.of("SUPER_ADMIN"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest();
        request.setTenantId(UUID.randomUUID());
        request.setEmail("cross-tenant@example.com");
        request.setFirstName("Cross");
        request.setLastName("Tenant");

        UserResponse response = userService.create(request);

        assertThat(response.getEmail()).isEqualTo("cross-tenant@example.com");
    }

    @Test
    void shouldNotReturnUserBelongingToAnotherTenant() {
        authenticate(tenantId, Set.of("TENANT_ADMIN"));
        UUID otherUsersId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(otherUsersId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(otherUsersId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRejectListByTenantForAnotherTenant() {
        authenticate(tenantId, Set.of("TENANT_ADMIN"));

        assertThatThrownBy(() -> userService.listByTenant(UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticate(UUID principalTenantId, Set<String> roles) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .tenantId(principalTenantId.toString())
                .roles(roles)
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        TenantContext.setTenantId(principalTenantId.toString());
        TenantContext.setPlatformAdmin(roles.contains("SUPER_ADMIN"));
    }
}
