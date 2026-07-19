package com.aisales.customer.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerDomainTest {

    @Test
    void shouldAbsorbFromDuplicateWithoutLosingSurvivorFields() {
        UUID tenant = UUID.randomUUID();
        Customer survivor = Customer.builder()
                .tenantId(tenant)
                .fullName("Survivor")
                .phone("+911")
                .email("keep@example.com")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .metadata(new HashMap<>())
                .preferences(new HashMap<>())
                .build();
        Customer loser = Customer.builder()
                .tenantId(tenant)
                .fullName("Loser")
                .phone("+922")
                .email("extra@example.com")
                .whatsapp("+933")
                .status(CustomerStatus.PROSPECT)
                .sourceType(CustomerSourceType.MANUAL)
                .metadata(new HashMap<>(java.util.Map.of("k", "v")))
                .preferences(new HashMap<>())
                .build();

        survivor.absorbFrom(loser, UUID.randomUUID());

        assertThat(survivor.getEmail()).isEqualTo("keep@example.com");
        assertThat(survivor.getWhatsapp()).isEqualTo("+933");
        assertThat(survivor.getMetadata()).containsEntry("k", "v");
    }

    @Test
    void shouldRejectCrossTenantMerge() {
        Customer survivor = Customer.builder()
                .tenantId(UUID.randomUUID())
                .fullName("A")
                .phone("+911")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .build();
        Customer loser = Customer.builder()
                .tenantId(UUID.randomUUID())
                .fullName("B")
                .phone("+922")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .build();

        assertThatThrownBy(() -> survivor.absorbFrom(loser, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("tenants");
    }

    @Test
    void shouldDeactivateAndReactivate() {
        Customer customer = Customer.builder()
                .tenantId(UUID.randomUUID())
                .fullName("A")
                .phone("+911")
                .status(CustomerStatus.ACTIVE)
                .sourceType(CustomerSourceType.MANUAL)
                .build();
        UUID actor = UUID.randomUUID();
        customer.deactivate(actor);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        customer.reactivate(actor);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
