package com.aisales.identity.application.service;

import com.aisales.common.exception.exception.ValidationException;
import com.aisales.identity.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Derives URL-safe tenant slugs from company names.
 */
@Component
@RequiredArgsConstructor
public class SlugGenerator {

    private static final int MIN_SLUG_LENGTH = 3;
    private static final int MAX_SLUG_LENGTH = 100;
    private static final int MAX_SUFFIX_ATTEMPTS = 99;

    private final TenantRepository tenantRepository;

    /**
     * Normalizes a company name into a URL-safe slug (no uniqueness check).
     */
    public String generate(String companyName) {
        String slug = normalize(companyName);
        if (slug.length() < MIN_SLUG_LENGTH) {
            throw new ValidationException("Company name must produce a valid slug (at least 3 characters)");
        }
        return slug;
    }

    /**
     * Returns a unique variant of {@code slug} by appending a numeric or random suffix.
     */
    public String generateUnique(String slug) {
        for (int suffix = 2; suffix <= MAX_SUFFIX_ATTEMPTS; suffix++) {
            String candidate = truncate(slug + "-" + suffix);
            if (!tenantRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        String candidate = truncate(slug + "-" + UUID.randomUUID().toString().substring(0, 8));
        if (tenantRepository.existsBySlug(candidate)) {
            throw new ValidationException("Unable to generate a unique tenant slug");
        }
        return candidate;
    }

    private static String normalize(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(companyName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s_-]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.length() > MAX_SLUG_LENGTH) {
            normalized = normalized.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return normalized;
    }

    private static String truncate(String slug) {
        if (slug.length() <= MAX_SLUG_LENGTH) {
            return slug;
        }
        return slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
    }
}
