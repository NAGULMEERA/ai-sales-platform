package com.aisales.media.infrastructure.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Media storage plug/flag: {@code aisales.media.storage} = {@code LOCAL} | {@code S3}.
 */
@Data
@ConfigurationProperties(prefix = "aisales.media")
public class MediaProperties {

    /** Active storage backend key. */
    private String storage = "LOCAL";

    /** Maximum upload size in bytes (aligned with servlet multipart defaults). */
    private long maxBytes = 25L * 1024 * 1024;

    /** When true, reject files whose magic bytes do not match the declared content type. */
    private boolean validateMagicBytes = true;

    /**
     * Allowlisted Content-Type values (lowercase, without parameters).
     * Empty list is not permitted at runtime — defaults are applied below.
     */
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain",
            "text/csv",
            "text/markdown",
            "application/json",
            "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"));

    private Local local = new Local();

    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String basePath = "./data/media";
    }

    @Data
    public static class S3 {
        private boolean enabled = false;
        private String bucket = "aisales-media";
        private String region = "us-east-1";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        /** When true, use path-style for MinIO/LocalStack. */
        private boolean pathStyleAccess = false;
    }
}
