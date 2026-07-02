package com.aisales.tenant.domain.service;

import com.aisales.common.contracts.tenant.SubscriptionPlan;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.tenant.domain.entity.Tenant;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TenantDomainService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern TENANT_CODE_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    public void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Tenant slug cannot be empty");
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new ValidationException("Slug must contain lowercase letters, numbers, and hyphens only");
        }
    }

    public String resolveTenantCode(String requestedCode, String slug) {
        String code = requestedCode != null && !requestedCode.isBlank()
                ? requestedCode.trim().toUpperCase(Locale.ROOT)
                : slug.toUpperCase(Locale.ROOT).replace('-', '_');
        if (!TENANT_CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException("Tenant code must contain uppercase letters, numbers, and underscores only");
        }
        return code;
    }

    public SubscriptionPlan resolveSubscriptionPlan(SubscriptionPlan plan) {
        return plan != null ? plan : SubscriptionPlan.FREE;
    }

    public String resolveTimezone(String timezone) {
        return timezone != null && !timezone.isBlank() ? timezone.trim() : "UTC";
    }

    public String resolveLanguage(String language) {
        return language != null && !language.isBlank() ? language.trim() : "en";
    }

    public void activate(Tenant tenant) {
        if (tenant.isDeleted()) {
            throw new ValidationException("Deleted tenant cannot be activated");
        }
        tenant.setStatus(TenantStatus.ACTIVE);
    }

    public void suspend(Tenant tenant) {
        if (tenant.isDeleted()) {
            throw new ValidationException("Deleted tenant cannot be suspended");
        }
        tenant.setStatus(TenantStatus.SUSPENDED);
    }

    public void softDelete(Tenant tenant, String deletedBy) {
        tenant.setDeleted(true);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant.setDeletedAt(java.time.Instant.now());
        tenant.setDeletedBy(deletedBy);
    }
}
