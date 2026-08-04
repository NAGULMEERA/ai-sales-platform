package com.aisales.identity.user.application;

import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.EmailNormalizer;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ForbiddenException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.security.model.UserPrincipal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;



@Service
@RequiredArgsConstructor
public class UserService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public UserDto createUser(UUID tenantId, CreateUserRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already registered");
        }
        Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of("USER");
        // Only an existing SUPER_ADMIN may grant the SUPER_ADMIN role. Without this check,
        // any caller permitted to create users (e.g. a TENANT_ADMIN) could self-escalate to
        // platform-wide admin by simply requesting roles=["SUPER_ADMIN"].
        if (roles.contains(SUPER_ADMIN) && !callerIsSuperAdmin()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN,
                    "Only a super admin may create users with the SUPER_ADMIN role");
        }
        User user = User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        user = userRepository.save(user);
        eventPublisher.publish(UserCreatedEvent.of(
                tenantId.toString(), user.getId().toString(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRoles(),
                CorrelationIdUtils.getCorrelationId()));
        auditService.logSecurityEvent(tenantId, user.getId(), AuditAction.USER_CREATED, "user",
                user.getId().toString(), null, null, AuditDetails.roles(user.getRoles()));
        return toDto(user);
    }

    private boolean callerIsSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        Set<String> callerRoles = principal.getRoles();
        return callerRoles != null && callerRoles.contains(SUPER_ADMIN);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(UUID id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("User", id));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
