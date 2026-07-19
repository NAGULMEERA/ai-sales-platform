package com.aisales.media.infrastructure.storage;

import com.aisales.media.domain.storage.ObjectStorage;
import com.aisales.media.infrastructure.configuration.MediaProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3-compatible object store for production. Selected when
 * {@code aisales.media.storage=S3} and {@code aisales.media.s3.enabled=true}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.media.s3.enabled", havingValue = "true")
public class S3ObjectStorage implements ObjectStorage {

    public static final String NAME = "S3";

    private final MediaProperties properties;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StoredObject put(String objectKey, byte[] content, String contentType) {
        MediaProperties.S3 config = properties.getS3();
        try (S3Client client = buildClient(config)) {
            PutObjectRequest.Builder request = PutObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(objectKey);
            if (StringUtils.hasText(contentType)) {
                request.contentType(contentType);
            }
            client.putObject(request.build(), RequestBody.fromBytes(content));
        }
        return new StoredObject(config.getBucket(), objectKey);
    }

    @Override
    public byte[] get(String objectKey) {
        MediaProperties.S3 config = properties.getS3();
        try (S3Client client = buildClient(config)) {
            return client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(config.getBucket())
                            .key(objectKey)
                            .build())
                    .asByteArray();
        }
    }

    private static S3Client buildClient(MediaProperties.S3 config) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(config.getRegion()));
        if (StringUtils.hasText(config.getEndpoint())) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }
        if (config.isPathStyleAccess()) {
            builder.forcePathStyle(true);
        }
        if (StringUtils.hasText(config.getAccessKey()) && StringUtils.hasText(config.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
