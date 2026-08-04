package com.aisales.identity.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.security.model.UserPrincipal;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;
import com.aisales.common.events.publisher.EventPublisher;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies the SUPER_ADMIN role-escalation guard: a caller must already hold SUPER_ADMIN
 * to create another user with that role, regardless of what permission grants them access
 * to the create-user endpoint itself (e.g. TENANT_ADMIN).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private UserService userService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, eventPublisher, auditService);
        tenantId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectSuperAdminRoleGrantFromNonSuperAdminCaller() {
        authenticate(Set.of("TENANT_ADMIN"));
        CreateUserRequest request = CreateUserRequest.builder()
                .email("escalate@example.com")
                .password("Password1!")
                .firstName("Eve")
                .lastName("Escalator")
                .roles(Set.of("SUPER_ADMIN"))
                .build();

        assertThatThrownBy(() -> userService.createUser(tenantId, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldAllowSuperAdminRoleGrantFromSuperAdminCaller() {
        authenticate(Set.of("SUPER_ADMIN"));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(java.time.Instant.now());
            return user;
        });
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newadmin@example.com")
                .password("Password1!")
                .firstName("New")
                .lastName("Admin")
                .roles(Set.of("SUPER_ADMIN"))
                .build();

        var result = userService.createUser(tenantId, request);

        assertThat(result.getRoles()).contains("SUPER_ADMIN");
    }

    @Test
    void shouldAllowOrdinaryUserCreationRegardlessOfCallerRole() {
        authenticate(Set.of("TENANT_ADMIN"));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(java.time.Instant.now());
            return user;
        });
        CreateUserRequest request = CreateUserRequest.builder()
                .email("member@example.com")
                .password("Password1!")
                .firstName("Team")
                .lastName("Member")
                .roles(Set.of("USER"))
                .build();

        var result = userService.createUser(tenantId, request);

        assertThat(result.getRoles()).containsExactly("USER");
    }

    private void authenticate(Set<String> roles) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .tenantId(tenantId.toString())
                .roles(roles)
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
