package ch.bbw.trips.storage.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SuppressWarnings("unchecked")
class S3StorageHealthIndicatorTest {
    final S3Client s3 = mock(S3Client.class);
    final S3StorageHealthIndicator health = new S3StorageHealthIndicator(s3,
            new S3StorageProperties("photos-bucket", "employee-photos/", "us-east-1", null));

    @Test
    void upWhenPrefixCanBeListed() {
        when(s3.listObjectsV2(any(Consumer.class))).thenReturn(ListObjectsV2Response.builder().build());
        assertEquals(Status.UP, health.health(true).getStatus());
    }

    @Test
    void downWithReasonWhenPolicyDeniesAccess() {
        when(s3.listObjectsV2(any(Consumer.class))).thenThrow(S3Exception.builder().statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build()).build());
        var result = health.health(true);
        assertEquals(Status.DOWN, result.getStatus());
        assertEquals("AccessDenied", result.getDetails().get("error"));
    }
}
