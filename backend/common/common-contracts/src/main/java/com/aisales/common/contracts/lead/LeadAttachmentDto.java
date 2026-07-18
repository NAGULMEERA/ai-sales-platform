package com.aisales.common.contracts.lead;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAttachmentDto {
    private UUID id;
    private UUID leadId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private UUID uploadedBy;
    private Instant uploadedAt;
}
