package com.aisales.media.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.media.domain.storage.ObjectStorage;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRoundTripBytes() throws Exception {
        MediaProperties properties = new MediaProperties();
        properties.setStorage("LOCAL");
        properties.getLocal().setBasePath(tempDir.toString());
        LocalObjectStorage storage = new LocalObjectStorage(properties);

        ObjectStorage.StoredObject stored =
                storage.put("tenant/obj/file.txt", "hello-media".getBytes(), "text/plain");
        assertThat(stored.objectKey()).isEqualTo("tenant/obj/file.txt");
        assertThat(storage.get(stored.objectKey())).isEqualTo("hello-media".getBytes());
        assertThat(Files.exists(tempDir.resolve("tenant/obj/file.txt"))).isTrue();
    }
}
