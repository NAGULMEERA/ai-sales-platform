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
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Collects payment for ISSUED invoices via the configured provider (STUB | STRIPE).
 * Provider HTTP calls run outside the persistence transaction.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final PlatformMetrics platformMetrics;
    private final PlatformTransactionManager transactionManager;

    public PaymentDto payInvoice(UUID invoiceId, PayInvoiceRequest request) {
        UUID tenantId = requireTenantId();
        InvoiceSnapshot snapshot = new TransactionTemplate(transactionManager).execute(status -> {
            Invoice invoice = requireIssuedPayableInvoice(tenantId, invoiceId);
            BigDecimal amount = invoice.getTotalUsd() != null ? invoice.getTotalUsd() : BigDecimal.ZERO;
            return new InvoiceSnapshot(invoiceId, tenantId, amount, invoice.getCurrency());
        });

        PaymentProvider provider = paymentProviderRegistry.resolveDefault();
        PaymentChargeResult result;
        if (snapshot.amountUsd().compareTo(BigDecimal.ZERO) <= 0) {
            result = PaymentChargeResult.succeeded("zero_" + invoiceId);
        } else {
            String paymentMethodId = request != null ? request.getPaymentMethodId() : null;
            // Provider HTTP must not hold a DB connection (Track A P1).
            result = provider.charge(new PaymentProvider.PaymentChargeRequest(
                    invoiceId,
                    tenantId,
                    snapshot.amountUsd(),
                    snapshot.currency(),
                    paymentMethodId));
        }

        PaymentChargeResult chargeResult = result;
        PaymentDto dto = new TransactionTemplate(transactionManager).execute(status ->
                persistChargeResult(snapshot, provider.name(), chargeResult));
        if (dto != null) {
            platformMetrics.incrementBusinessMetric(
                    MetricNames.BILLING_PAYMENT_CREATED,
                    tenantId.toString(),
                    "provider", provider.name(),
                    "status", dto.getStatus().name());
        }
        return dto;
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

    /**
     * Marks a PENDING payment FAILED when the provider reports failure (e.g. Stripe webhook).
     * Idempotent for already-FAILED; never downgrades SUCCEEDED.
     *
     * @return true when a payment row was found
     */
    @Transactional
    public boolean markFailedByProviderPaymentId(String providerPaymentId, String failureMessage) {
        if (!StringUtils.hasText(providerPaymentId)) {
            return false;
        }
        Optional<Payment> found = paymentRepository.findByProviderPaymentId(providerPaymentId);
        if (found.isEmpty()) {
            return false;
        }
        Payment payment = found.get();
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Payment " + payment.getId() + " already succeeded and cannot be marked failed");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Payment " + payment.getId() + " is " + payment.getStatus() + " and cannot be marked failed");
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureMessage(truncate(failureMessage, 1000));
        payment.setClientSecret(null);
        payment.setUpdatedAt(now);
        payment.setUpdatedBy("stripe-webhook");
        paymentRepository.save(payment);
        return true;
    }

    private PaymentDto persistChargeResult(
            InvoiceSnapshot snapshot, String providerName, PaymentChargeResult result) {
        Invoice invoice = requireIssuedPayableInvoice(snapshot.tenantId(), snapshot.invoiceId());
        Instant now = Instant.now();
        String actor = actor();

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
                .tenantId(snapshot.tenantId())
                .invoiceId(snapshot.invoiceId())
                .status(status)
                .provider(providerName)
                .providerPaymentId(result.providerPaymentId())
                .currency(snapshot.currency())
                .amountUsd(snapshot.amountUsd())
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

    private Invoice requireIssuedPayableInvoice(UUID tenantId, UUID invoiceId) {
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
        return invoice;
    }

    private static String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "payment_failed";
        }
        return value.length() <= max ? value : value.substring(0, max);
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

    private record InvoiceSnapshot(UUID invoiceId, UUID tenantId, BigDecimal amountUsd, String currency) {
    }
}
