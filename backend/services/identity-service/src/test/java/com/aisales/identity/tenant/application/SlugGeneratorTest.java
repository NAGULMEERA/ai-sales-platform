package com.aisales.identity.tenant.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aisales.identity.tenant.infrastructure.persistence.TenantRepository;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlugGeneratorTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private SlugGenerator slugGenerator;

    @Test
    void shouldGenerateSlugFromCompanyName() {
        assertThat(slugGenerator.generate("Acme Corp")).isEqualTo("acme-corp");
        assertThat(slugGenerator.generate("  My Company!!!  ")).isEqualTo("my-company");
    }

    @Test
    void shouldGenerateUniqueSlugWithSuffix() {
        when(tenantRepository.existsBySlug("acme-corp-2")).thenReturn(false);

        assertThat(slugGenerator.generateUnique("acme-corp")).isEqualTo("acme-corp-2");
    }

    @Test
    void shouldIncrementSuffixUntilAvailable() {
        when(tenantRepository.existsBySlug("acme-corp-2")).thenReturn(true);
        when(tenantRepository.existsBySlug("acme-corp-3")).thenReturn(false);

        assertThat(slugGenerator.generateUnique("acme-corp")).isEqualTo("acme-corp-3");
    }
}
