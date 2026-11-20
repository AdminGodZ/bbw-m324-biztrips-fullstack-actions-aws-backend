package ch.bbw.trips.storage.s3;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Health contributor "s3": lists at most one key below the photo prefix.
 * That is exactly the s3:ListBucket permission the least-privilege policy grants (with s3:prefix condition),
 * so a missing bucket or a wrong policy makes the task unready and the ECS deployment circuit breaker
 * rolls the release back instead of serving a broken backend.
 */
public class S3StorageHealthIndicator extends AbstractHealthIndicator {
    private final S3Client s3;
    private final S3StorageProperties properties;

    public S3StorageHealthIndicator(S3Client s3, S3StorageProperties properties) {
        super("S3 health check failed");
        this.s3 = s3;
        this.properties = properties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            s3.listObjectsV2(b -> b.bucket(properties.bucket()).prefix(properties.prefix()).maxKeys(1));
            builder.up().withDetail("prefix", properties.prefix());
        } catch (S3Exception ex) {
            String code = ex.awsErrorDetails() == null ? "unknown" : ex.awsErrorDetails().errorCode();
            builder.down().withDetail("error", code).withDetail("status", ex.statusCode());
        } catch (SdkClientException ex) {
            builder.down().withDetail("error", "unreachable");
        }
    }
}
