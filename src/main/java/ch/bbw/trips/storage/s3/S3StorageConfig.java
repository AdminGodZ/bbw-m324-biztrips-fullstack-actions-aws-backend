package ch.bbw.trips.storage.s3;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Creates the S3 client. Credentials come from the default provider chain:
 * on ECS Fargate this is the task role (container credentials endpoint) - no access keys in the app.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(S3StorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(4))
                        .build());
        if (properties.endpointUri() != null) {
            // Local S3 emulator (Docker Compose): custom endpoint with path-style URLs.
            builder.endpointOverride(properties.endpointUri()).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    S3PhotoStorage s3PhotoStorage(S3Client s3, S3StorageProperties properties) {
        return new S3PhotoStorage(s3, properties);
    }

    @Bean
    S3StorageHealthIndicator s3HealthIndicator(S3Client s3, S3StorageProperties properties) {
        return new S3StorageHealthIndicator(s3, properties);
    }
}
