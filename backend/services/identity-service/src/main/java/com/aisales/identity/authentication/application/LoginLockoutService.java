package com.aisales.identity.authentication.application;

import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists failed-login counters and lockouts in an independent transaction so increments
 * survive rollback of the failed login attempt.
 */
@Service
@RequiredArgsConstructor
public class LoginLockoutService {

    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    public boolean isCurrentlyLocked(User user) {
        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            return true;
        }
        // Permanent/admin lock (LOCKED without a timed expiry).
        return user.getStatus() == User.UserStatus.LOCKED && user.getLockedUntil() == null;
    }

    /**
     * Clears an expired timed lockout so the user can authenticate again.
     */
    @Transactional
    public void unlockIfExpired(User user) {
        Instant now = Instant.now();
        boolean expiredLock = user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now);
        if (user.getStatus() == User.UserStatus.LOCKED && expiredLock) {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        } else if (expiredLock && user.getFailedLoginAttempts() > 0) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedPasswordAttempt(UUID userId, String ipAddress, String userAgent) {
        // Pessimistic lock so concurrent failures cannot lose increments via @Version races.
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) {
            return;
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= authProperties.getMaxFailedLoginAttempts()) {
            Instant lockedUntil = Instant.now()
                    .plusSeconds(authProperties.getLockoutDurationMinutes() * 60L);
            user.setStatus(User.UserStatus.LOCKED);
            user.setLockedUntil(lockedUntil);
            userRepository.save(user);
            auditService.logSecurityEventIndependent(
                    user.getTenantId(),
                    user.getId(),
                    AuditAction.ACCOUNT_LOCKED,
                    "user",
                    user.getId().toString(),
                    ipAddress,
                    userAgent,
                    AuditDetails.reason("max_failed_login_attempts"));
            return;
        }
        userRepository.save(user);
    }

    @Transactional
    public void clearFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() == 0
                && user.getLockedUntil() == null
                && user.getStatus() != User.UserStatus.LOCKED) {
            return;
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }
}
