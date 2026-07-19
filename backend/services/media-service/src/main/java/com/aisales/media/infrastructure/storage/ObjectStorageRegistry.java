package com.aisales.media.infrastructure.storage;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.media.domain.storage.ObjectStorage;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes binary storage by {@code aisales.media.storage} ({@code LOCAL} | {@code S3}).
 */
@Component
@RequiredArgsConstructor
public class ObjectStorageRegistry {

    private final List<ObjectStorage> storages;
    private final MediaProperties properties;

    public ObjectStorage resolveDefault() {
        return resolveByName(properties.getStorage());
    }

    public ObjectStorage resolveByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "aisales.media.storage is not set");
        }
        String key = name.trim().toUpperCase(Locale.ROOT);
        return storages.stream()
                .filter(s -> key.equals(s.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "No media storage registered for provider="
                                + key
                                + ". Available: "
                                + storages.stream()
                                        .map(ObjectStorage::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                                + ". For S3 set aisales.media.storage=S3 and aisales.media.s3.enabled=true."));
    }
}
