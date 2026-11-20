package ch.bbw.trips.storage.s3;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * S3 settings, all supplied via environment variables (see application-s3.properties).
 *
 * @param bucket   bucket name, e.g. bbw-m324-refcard05-photos-123456789012 (APP_STORAGE_S3_BUCKET)
 * @param prefix   key prefix the task role is restricted to, e.g. employee-photos/ (APP_STORAGE_S3_PREFIX)
 * @param region   AWS region (AWS_REGION)
 * @param endpoint optional endpoint override for a local S3 emulator; empty on AWS (APP_STORAGE_S3_ENDPOINT)
 */
@ConfigurationProperties("app.storage.s3")
public record S3StorageProperties(String bucket, String prefix, String region, String endpoint) {

    public S3StorageProperties {
        if (!StringUtils.hasText(bucket))
            throw new IllegalStateException("app.storage.type=s3 requires APP_STORAGE_S3_BUCKET");
        if (!StringUtils.hasText(region))
            throw new IllegalStateException("app.storage.type=s3 requires AWS_REGION");
        prefix = StringUtils.hasText(prefix) ? (prefix.endsWith("/") ? prefix : prefix + "/") : "";
        endpoint = StringUtils.hasText(endpoint) ? endpoint : null;
    }

    public String key(String filename) {
        return prefix + filename;
    }

    public URI endpointUri() {
        return endpoint == null ? null : URI.create(endpoint);
    }
}
