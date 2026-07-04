package com.aisales.identity.user.application;

import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public UserDto createUser(UUID tenantId, CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered");
        }
        Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of("USER");
        User user = User.builder()
                .tenantId(tenantId)
                .email(request.getEmail())
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
