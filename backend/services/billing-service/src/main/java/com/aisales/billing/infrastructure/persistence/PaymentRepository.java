package com.aisales.billing.infrastructure.persistence;

import com.aisales.billing.domain.entity.Payment;
import com.aisales.common.contracts.billing.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);
}
