package com.aisales.media.application.service;

import com.aisales.common.contracts.media.MediaDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.media.domain.entity.MediaObject;
import com.aisales.media.domain.storage.ObjectStorage;
import com.aisales.media.infrastructure.persistence.MediaObjectRepository;
import com.aisales.media.infrastructure.storage.ObjectStorageRegistry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaApplicationService {

    private final MediaObjectRepository mediaObjectRepository;
    private final ObjectStorageRegistry objectStorageRegistry;
    private final MediaContentValidator mediaContentValidator;

    @Transactional
    public MediaDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }
        UUID tenantId = requireTenantId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new ValidationException("Unable to read uploaded file");
        }
        mediaContentValidator.validate(file, bytes);

        ObjectStorage storage = objectStorageRegistry.resolveDefault();
        UUID mediaId = UUID.randomUUID();
        String objectKey = tenantId + "/" + mediaId + "/" + sanitizeFilename(file.getOriginalFilename());
        String contentType = MediaContentValidator.normalizeContentType(file.getContentType());
        ObjectStorage.StoredObject stored =
                storage.put(objectKey, bytes, contentType);

        Instant now = Instant.now();
        String actor = StringUtils.hasText(TenantContext.getUserId())
                ? TenantContext.getUserId()
                : "system";
        MediaObject saved = mediaObjectRepository.save(MediaObject.builder()
                .id(mediaId)
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .originalFilename(file.getOriginalFilename())
                .contentType(contentType)
                .sizeBytes((long) bytes.length)
                .checksumSha256(sha256(bytes))
                .bucketName(stored.bucketName())
                .objectKey(stored.objectKey())
                .storageProvider(storage.name())
                .status("UPLOADED")
                .createdAt(now)
                .createdBy(actor)
                .updatedAt(now)
                .updatedBy(actor)
                .version(0L)
                .build());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public MediaDto get(UUID id) {
        return toDto(requireOwned(id));
    }

    @Transactional(readOnly = true)
    public byte[] downloadContent(UUID id) {
        MediaObject media = requireOwned(id);
        return objectStorageRegistry.resolveByName(media.getStorageProvider()).get(media.getObjectKey());
    }

    private MediaObject requireOwned(UUID id) {
        UUID tenantId = requireTenantId();
        return mediaObjectRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Media object not found: " + id));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static MediaDto toDto(MediaObject media) {
        return MediaDto.builder()
                .id(media.getId())
                .tenantId(media.getTenantId())
                .originalFilename(media.getOriginalFilename())
                .contentType(media.getContentType())
                .sizeBytes(media.getSizeBytes())
                .checksumSha256(media.getChecksumSha256())
                .bucketName(media.getBucketName())
                .objectKey(media.getObjectKey())
                .storageProvider(media.getStorageProvider())
                .status(media.getStatus())
                .createdAt(media.getCreatedAt())
                .build();
    }

    private static String sanitizeFilename(String name) {
        if (!StringUtils.hasText(name)) {
            return "bin";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
