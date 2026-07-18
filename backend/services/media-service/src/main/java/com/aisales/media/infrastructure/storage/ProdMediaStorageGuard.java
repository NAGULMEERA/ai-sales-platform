package com.aisales.media.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-fast in production when local filesystem media storage is still selected.
 */
@Component
@Profile("prod")
public class ProdMediaStorageGuard implements ApplicationRunner {

    private final String storage;

    public ProdMediaStorageGuard(@Value("${aisales.media.storage:LOCAL}") String storage) {
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (storage == null || "LOCAL".equalsIgnoreCase(storage.trim())) {
            throw new IllegalStateException(
                    "aisales.media.storage=LOCAL is forbidden when spring.profiles.active includes prod. "
                            + "Set aisales.media.storage=S3 and aisales.media.s3.enabled=true before deploying.");
        }
    }
}
