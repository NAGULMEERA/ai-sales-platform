package com.aisales.common.contracts.media;

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
public class MediaDto {

    private UUID id;
    private UUID tenantId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String checksumSha256;
    private String bucketName;
    private String objectKey;
    private String storageProvider;
    private String status;
    private Instant createdAt;
}
