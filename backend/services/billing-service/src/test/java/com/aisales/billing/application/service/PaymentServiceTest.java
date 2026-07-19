package com.aisales.billing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.billing.domain.entity.Invoice;
import com.aisales.billing.domain.entity.Payment;
import com.aisales.billing.infrastructure.payment.PaymentProviderRegistry;
import com.aisales.billing.infrastructure.payment.StubPaymentProvider;
import com.aisales.billing.infrastructure.persistence.InvoiceRepository;
import com.aisales.billing.infrastructure.persistence.PaymentRepository;
import com.aisales.common.contracts.billing.InvoiceStatus;
import com.aisales.common.contracts.billing.PayInvoiceRequest;
import com.aisales.common.contracts.billing.PaymentDto;
import com.aisales.common.contracts.billing.PaymentStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.PlatformMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;

    private PaymentService service;
    private UUID tenantId;
    private UUID invoiceId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        PaymentProviderRegistry registry = new PaymentProviderRegistry(
                List.of(new StubPaymentProvider()),
                new com.aisales.billing.infrastructure.configuration.PaymentProperties());
        service = new PaymentService(
                invoiceRepository,
                paymentRepository,
                registry,
                new PlatformMetrics(new SimpleMeterRegistry()),
                passthroughTransactionManager());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPayIssuedInvoiceAndMarkPaid() {
        when(invoiceRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, invoiceId))
                .thenReturn(Optional.of(issuedInvoice()));
        when(paymentRepository.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCEEDED)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto dto = service.payInvoice(invoiceId, PayInvoiceRequest.builder().build());

        assertThat(dto.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(dto.getProvider()).isEqualTo("STUB");
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoiceCaptor.getValue().getPaidAt()).isNotNull();
    }

    @Test
    void shouldCompletePendingPaymentViaProviderId() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findByProviderPaymentId("pi_abc")).thenReturn(Optional.of(Payment.builder()
                .id(paymentId)
                .tenantId(tenantId)
                .invoiceId(invoiceId)
                .status(PaymentStatus.PENDING)
                .provider("STRIPE")
                .providerPaymentId("pi_abc")
                .currency("USD")
                .amountUsd(new BigDecimal("1.25"))
                .clientSecret("secret")
                .createdAt(Instant.now())
                .createdBy("system")
                .updatedAt(Instant.now())
                .updatedBy("system")
                .version(0L)
                .build()));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(issuedInvoice()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean found = service.completeSucceededByProviderPaymentId("pi_abc");

        assertThat(found).isTrue();
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentCaptor.getValue().getClientSecret()).isNull();
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void shouldMarkPendingPaymentFailedViaProviderId() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findByProviderPaymentId("pi_fail")).thenReturn(Optional.of(Payment.builder()
                .id(paymentId)
                .tenantId(tenantId)
                .invoiceId(invoiceId)
                .status(PaymentStatus.PENDING)
                .provider("STRIPE")
                .providerPaymentId("pi_fail")
                .currency("USD")
                .amountUsd(new BigDecimal("1.25"))
                .clientSecret("secret")
                .createdAt(Instant.now())
                .createdBy("system")
                .updatedAt(Instant.now())
                .updatedBy("system")
                .version(0L)
                .build()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean found = service.markFailedByProviderPaymentId("pi_fail", "card_declined");

        assertThat(found).isTrue();
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(paymentCaptor.getValue().getFailureMessage()).isEqualTo("card_declined");
        assertThat(paymentCaptor.getValue().getClientSecret()).isNull();
    }

    @Test
    void shouldRejectDraftInvoice() {
        when(invoiceRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, invoiceId))
                .thenReturn(Optional.of(Invoice.builder()
                        .id(invoiceId)
                        .tenantId(tenantId)
                        .status(InvoiceStatus.DRAFT)
                        .currency("USD")
                        .source("AI_USAGE")
                        .totalUsd(BigDecimal.ONE)
                        .periodStart(Instant.parse("2026-07-01T00:00:00Z"))
                        .periodEnd(Instant.parse("2026-08-01T00:00:00Z"))
                        .createdAt(Instant.now())
                        .createdBy("system")
                        .updatedAt(Instant.now())
                        .updatedBy("system")
                        .version(0L)
                        .build()));

        assertThatThrownBy(() -> service.payInvoice(invoiceId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ISSUED");
    }

    private Invoice issuedInvoice() {
        return Invoice.builder()
                .id(invoiceId)
                .tenantId(tenantId)
                .status(InvoiceStatus.ISSUED)
                .currency("USD")
                .source("AI_USAGE")
                .totalUsd(new BigDecimal("1.25"))
                .periodStart(Instant.parse("2026-07-01T00:00:00Z"))
                .periodEnd(Instant.parse("2026-08-01T00:00:00Z"))
                .createdAt(Instant.now())
                .createdBy("system")
                .updatedAt(Instant.now())
                .updatedBy("system")
                .version(0L)
                .build();
    }

    private static PlatformTransactionManager passthroughTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus(true);
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
