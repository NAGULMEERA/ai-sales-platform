package com.aisales.common.cache;

import com.aisales.common.core.util.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantCacheKeyGeneratorTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private TenantCacheKeyGenerator generator;

    @BeforeEach
    void setUp() {
        CacheProperties properties = new CacheProperties();
        properties.setKeyPrefix("aisales");
        generator = new TenantCacheKeyGenerator(properties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldBuildTenantScopedKey() {
        TenantContext.setTenantId(TENANT_ID.toString());

        assertThat(generator.key("tenant", "abc"))
                .isEqualTo("aisales:tenant:" + TENANT_ID + ":tenant:abc");
    }

    @Test
    void shouldBuildPlatformKeyWhenNoTenant() {
        assertThat(generator.key("config", "feature-flags"))
                .isEqualTo("aisales:platform:config:feature-flags");
    }

    @Test
    void shouldBuildNamespacePatternForTenant() {
        TenantContext.setTenantId(TENANT_ID.toString());

        assertThat(generator.namespacePattern("tenant"))
                .isEqualTo("aisales:tenant:" + TENANT_ID + ":tenant:*");
    }

    @Test
    void shouldBuildTenantPatternForTenantWideInvalidation() {
        TenantContext.setTenantId(TENANT_ID.toString());

        assertThat(generator.tenantPattern())
                .isEqualTo("aisales:tenant:" + TENANT_ID + ":*");
    }
}
