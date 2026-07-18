package com.aisales.billing.application.service;

import com.aisales.billing.domain.entity.Invoice;
import com.aisales.billing.domain.entity.Payment;
import com.aisales.billing.domain.payment.PaymentProvider;
import com.aisales.billing.domain.payment.PaymentProvider.PaymentChargeResult;
import com.aisales.billing.infrastructure.payment.PaymentProviderRegistry;
import com.aisales.billing.infrastructure.persistence.InvoiceRepository;
import com.aisales.billing.infrastructure.persistence.PaymentRepository;
import com.aisales.common.contracts.billing.InvoiceStatus;
import com.aisales.common.contracts.billing.PayInvoiceRequest;
import com.aisales.common.contracts.billing.PaymentDto;
import com.aisales.common.contracts.billing.PaymentStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Collects payment for ISSUED invoices via the configured provider (STUB | STRIPE).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;

    @Transactional
    public PaymentDto payInvoice(UUID invoiceId, PayInvoiceRequest request) {
        UUID tenantId = requireTenantId();
        Invoice invoice = invoiceRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "Invoice is already paid");
        }
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ValidationException("Only ISSUED invoices can be paid");
        }
        if (paymentRepository.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCEEDED)) {
            throw new BusinessException(ErrorCode.CONFLICT, "A succeeded payment already exists for this invoice");
        }

        Instant now = Instant.now();
        String actor = actor();
        BigDecimal amount = invoice.getTotalUsd() != null ? invoice.getTotalUsd() : BigDecimal.ZERO;
        PaymentProvider provider = paymentProviderRegistry.resolveDefault();

        PaymentChargeResult result;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            result = PaymentChargeResult.succeeded("zero_" + invoiceId);
        } else {
            String paymentMethodId = request != null ? request.getPaymentMethodId() : null;
            result = provider.charge(new PaymentProvider.PaymentChargeRequest(
                    invoiceId, tenantId, amount, invoice.getCurrency(), paymentMethodId));
        }

        PaymentStatus status;
        Instant paidAt = null;
        if (result.succeeded()) {
            status = PaymentStatus.SUCCEEDED;
            paidAt = now;
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(now);
            invoice.setUpdatedAt(now);
            invoice.setUpdatedBy(actor);
            invoiceRepository.save(invoice);
        } else if (result.pendingClientAction()) {
            status = PaymentStatus.PENDING;
        } else {
            status = PaymentStatus.FAILED;
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .invoiceId(invoiceId)
                .status(status)
                .provider(provider.name())
                .providerPaymentId(result.providerPaymentId())
                .currency(invoice.getCurrency())
                .amountUsd(amount)
                .clientSecret(result.clientSecret())
                .failureMessage(result.failureMessage())
                .paidAt(paidAt)
                .createdAt(now)
                .createdBy(actor)
                .updatedAt(now)
                .updatedBy(actor)
                .version(0L)
                .build());
        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto get(UUID paymentId) {
        UUID tenantId = requireTenantId();
        return paymentRepository
                .findByTenantIdAndId(tenantId, paymentId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
    }

    /**
     * Completes a PENDING payment when the provider confirms success (e.g. Stripe webhook).
     * Idempotent: already-SUCCEEDED payments are left unchanged.
     *
     * @return true when a payment row was found (whether or not status changed)
     */
    @Transactional
    public boolean completeSucceededByProviderPaymentId(String providerPaymentId) {
        if (!StringUtils.hasText(providerPaymentId)) {
            return false;
        }
        Optional<Payment> found = paymentRepository.findByProviderPaymentId(providerPaymentId);
        if (found.isEmpty()) {
            return false;
        }
        Payment payment = found.get();
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return true;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Payment " + payment.getId() + " is " + payment.getStatus() + " and cannot be completed");
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(now);
        payment.setClientSecret(null);
        payment.setFailureMessage(null);
        payment.setUpdatedAt(now);
        payment.setUpdatedBy("stripe-webhook");
        paymentRepository.save(payment);

        Invoice invoice = invoiceRepository
                .findById(payment.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + payment.getInvoiceId()));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(now);
            invoice.setUpdatedAt(now);
            invoice.setUpdatedBy("stripe-webhook");
            invoiceRepository.save(invoice);
        }
        return true;
    }

    private PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoiceId())
                .tenantId(payment.getTenantId())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .providerPaymentId(payment.getProviderPaymentId())
                .currency(payment.getCurrency())
                .amountUsd(payment.getAmountUsd())
                .clientSecret(payment.getClientSecret())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static String actor() {
        return StringUtils.hasText(TenantContext.getUserId()) ? TenantContext.getUserId() : "system";
    }
}
