package com.aisales.media.domain.storage;

/**
 * Pluggable binary store. Selected by {@code aisales.media.storage}.
 */
public interface ObjectStorage {

    String name();

    StoredObject put(String objectKey, byte[] content, String contentType);

    byte[] get(String objectKey);

    record StoredObject(String bucketName, String objectKey) {
    }
}
