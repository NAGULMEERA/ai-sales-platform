package com.aisales.tenant.domain.service;

import com.aisales.tenant.domain.repository.TenantRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantSlugGenerator {

    private static final int MIN_SLUG_LENGTH = 3;
    private static final int MAX_SLUG_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 99;

    private final TenantRepositoryPort tenantRepository;
    private final TenantDomainService tenantDomainService;

    public String resolveSlug(String name, String requestedSlug) {
        String slug = requestedSlug != null && !requestedSlug.isBlank()
                ? requestedSlug.trim().toLowerCase(Locale.ROOT)
                : normalize(name);
        tenantDomainService.validateSlug(slug);
        return ensureUnique(slug);
    }

    private String ensureUnique(String slug) {
        if (!tenantRepository.existsBySlug(slug)) {
            return slug;
        }
        for (int suffix = 2; suffix <= MAX_SUFFIX_ATTEMPTS; suffix++) {
            String candidate = truncate(slug + "-" + suffix);
            if (!tenantRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        String candidate = truncate(slug + "-" + UUID.randomUUID().toString().substring(0, 8));
        if (tenantRepository.existsBySlug(candidate)) {
            throw new com.aisales.common.exception.exception.ValidationException("Unable to generate a unique tenant slug");
        }
        return candidate;
    }

    static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s_-]+", "-")
                .replaceAll("^-+|-+$", "");
        return truncate(normalized);
    }

    private static String truncate(String slug) {
        if (slug.length() <= MAX_SLUG_LENGTH) {
            return slug;
        }
        return slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
    }
}
