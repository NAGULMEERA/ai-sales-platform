package com.aisales.media.infrastructure.storage;

import com.aisales.media.domain.storage.ObjectStorage;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local filesystem store for development. Selected when {@code aisales.media.storage=LOCAL}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.media.storage", havingValue = "LOCAL", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {

    public static final String NAME = "LOCAL";

    private final MediaProperties properties;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StoredObject put(String objectKey, byte[] content, String contentType) {
        Path path = resolve(objectKey);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write local media object: " + objectKey, ex);
        }
        return new StoredObject("local", objectKey);
    }

    @Override
    public byte[] get(String objectKey) {
        Path path = resolve(objectKey);
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read local media object: " + objectKey, ex);
        }
    }

    private Path resolve(String objectKey) {
        return Path.of(properties.getLocal().getBasePath()).resolve(objectKey).normalize();
    }
}
