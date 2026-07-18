package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.domain.entity.KnowledgeBase;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.CreateKnowledgeBaseRequest;
import com.aisales.common.contracts.ai.KnowledgeBaseDto;
import com.aisales.common.contracts.ai.KnowledgeBaseStatus;
import com.aisales.common.contracts.ai.KnowledgeDocumentStatus;
import com.aisales.common.contracts.ai.RegisterKnowledgeDocumentRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.KnowledgeBaseCreatedEvent;
import com.aisales.common.events.model.KnowledgeDocumentRegisteredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private EventPublisher eventPublisher;

    private KnowledgeService knowledgeService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        knowledgeService = new KnowledgeService(
                knowledgeBaseRepository,
                knowledgeDocumentRepository,
                new AiMapper(),
                eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateKnowledgeBase() {
        when(knowledgeBaseRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "DEFAULT"))
                .thenReturn(false);
        when(knowledgeBaseRepository.saveAndFlush(any(KnowledgeBase.class))).thenAnswer(inv -> {
            KnowledgeBase kb = inv.getArgument(0);
            kb.setId(UUID.randomUUID());
            return kb;
        });

        KnowledgeBaseDto dto = knowledgeService.createBase(CreateKnowledgeBaseRequest.builder()
                .code("default")
                .name("Default KB")
                .description("Tenant FAQ")
                .build());

        assertThat(dto.getCode()).isEqualTo("DEFAULT");
        assertThat(dto.getStatus()).isEqualTo(KnowledgeBaseStatus.ACTIVE);
        ArgumentCaptor<KnowledgeBaseCreatedEvent> captor =
                ArgumentCaptor.forClass(KnowledgeBaseCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("KnowledgeBaseCreated");
    }

    @Test
    void shouldRegisterDocumentMetadata() {
        UUID kbId = UUID.randomUUID();
        KnowledgeBase kb = KnowledgeBase.builder()
                .id(kbId)
                .tenantId(tenantId)
                .code("DEFAULT")
                .name("Default")
                .status(KnowledgeBaseStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, kbId))
                .thenReturn(Optional.of(kb));
        when(knowledgeDocumentRepository.saveAndFlush(any(KnowledgeDocument.class))).thenAnswer(inv -> {
            KnowledgeDocument doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        UUID mediaId = UUID.randomUUID();
        var dto = knowledgeService.registerDocument(kbId, RegisterKnowledgeDocumentRequest.builder()
                .name("FAQ.pdf")
                .contentType("application/pdf")
                .mediaId(mediaId)
                .build());

        assertThat(dto.getStatus()).isEqualTo(KnowledgeDocumentStatus.PENDING);
        assertThat(dto.getMediaId()).isEqualTo(mediaId);
        ArgumentCaptor<KnowledgeDocumentRegisteredEvent> captor =
                ArgumentCaptor.forClass(KnowledgeDocumentRegisteredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("KnowledgeDocumentRegistered");
    }

    @Test
    void shouldRequireMediaOrObjectKey() {
        UUID kbId = UUID.randomUUID();
        when(knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, kbId))
                .thenReturn(Optional.of(KnowledgeBase.builder()
                        .id(kbId)
                        .tenantId(tenantId)
                        .code("DEFAULT")
                        .name("Default")
                        .status(KnowledgeBaseStatus.ACTIVE)
                        .createdAt(java.time.Instant.now())
                        .updatedAt(java.time.Instant.now())
                        .build()));

        assertThatThrownBy(() -> knowledgeService.registerDocument(kbId,
                        RegisterKnowledgeDocumentRequest.builder().name("x").build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("mediaId or objectKey");
    }
}
