package com.aisales.media.infrastructure.configuration;

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
