package com.aisales.tenant.domain.service;

import com.aisales.tenant.domain.repository.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSlugGeneratorTest {

    @Mock
    private TenantRepositoryPort tenantRepository;

    private TenantSlugGenerator slugGenerator;

    @BeforeEach
    void setUp() {
        slugGenerator = new TenantSlugGenerator(tenantRepository, new TenantDomainService());
    }

    @Test
    void shouldGenerateSlugFromCompanyName() {
        when(tenantRepository.existsBySlug("my-first-company")).thenReturn(false);

        String slug = slugGenerator.resolveSlug("My First Company", null);

        assertThat(slug).isEqualTo("my-first-company");
    }

    @Test
    void shouldAppendSuffixWhenSlugExists() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);
        when(tenantRepository.existsBySlug("acme-2")).thenReturn(false);

        String slug = slugGenerator.resolveSlug("Acme", "acme");

        assertThat(slug).isEqualTo("acme-2");
    }
}
