package ch.bbw.trips.storage.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.bbw.trips.storage.StorageUnavailableException;
import java.util.Base64;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@SuppressWarnings("unchecked")
class S3PhotoStorageTest {
    static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=");
    S3Client s3;
    S3PhotoStorage storage;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        storage = new S3PhotoStorage(s3, new S3StorageProperties("photos-bucket", "employee-photos", "us-east-1", ""));
    }

    static S3Exception s3Error(int status, String code) {
        return (S3Exception) S3Exception.builder().statusCode(status)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).build();
    }

    @Test
    void storesValidatedPhotoUnderPrefixWithContentType() throws Exception {
        String name = storage.store(new MockMultipartFile("file", "../../me.png", "image/png", PNG));

        var request = ArgumentCaptor.forClass(Consumer.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        var builder = PutObjectRequest.builder();
        request.getValue().accept(builder);
        PutObjectRequest put = builder.build();
        assertEquals("photos-bucket", put.bucket());
        assertEquals("employee-photos/" + name, put.key(), "key must stay below the least-privilege prefix");
        assertEquals("image/png", put.contentType());
        assertTrue(name.matches("[a-f0-9-]{36}\\.png"), "original file name must not be used");
    }

    @Test
    void rejectsSpoofedImageWithoutCallingS3() {
        assertThrows(ResponseStatusException.class, () ->
                storage.store(new MockMultipartFile("file", "x.png", "image/png", "no png".getBytes())));
        verifyNoInteractions(s3);
    }

    @Test
    void loadsPhotoBytes() throws Exception {
        String name = "0b6f0b1e-7d2a-4c55-9a51-6d3b4f6f1a2b.png";
        when(s3.getObjectAsBytes(any(Consumer.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), PNG));
        assertArrayEquals(PNG, storage.load(name).getContentAsByteArray());
    }

    @Test
    void missingObjectIs404() {
        when(s3.getObjectAsBytes(any(Consumer.class))).thenThrow(NoSuchKeyException.builder().statusCode(404).build());
        var error = assertThrows(ResponseStatusException.class,
                () -> storage.load("0b6f0b1e-7d2a-4c55-9a51-6d3b4f6f1a2b.png"));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void accessDeniedBecomesClearStorageError() {
        when(s3.putObject(any(Consumer.class), any(RequestBody.class))).thenThrow(s3Error(403, "AccessDenied"));
        var error = assertThrows(StorageUnavailableException.class,
                () -> storage.store(new MockMultipartFile("file", "a.png", "image/png", PNG)));
        assertEquals("ACCESS_DENIED", error.getReason());
        assertTrue(error.getMessage().contains("least privilege"), error.getMessage());
    }

    @Test
    void missingBucketAndNetworkErrorsAreReported() {
        when(s3.getObjectAsBytes(any(Consumer.class))).thenThrow(s3Error(404, "NoSuchBucket"));
        assertEquals("BUCKET_NOT_FOUND", assertThrows(StorageUnavailableException.class,
                () -> storage.load("0b6f0b1e-7d2a-4c55-9a51-6d3b4f6f1a2b.jpg")).getReason());

        when(s3.deleteObject(any(Consumer.class))).thenThrow(SdkClientException.create("connect timed out"));
        assertEquals("UNREACHABLE", assertThrows(StorageUnavailableException.class,
                () -> storage.delete("0b6f0b1e-7d2a-4c55-9a51-6d3b4f6f1a2b.jpg")).getReason());
    }

    @Test
    void rejectsKeyTraversalAndIgnoresNull() {
        assertThrows(ResponseStatusException.class, () -> storage.load("../other-prefix/secret.png"));
        assertThrows(ResponseStatusException.class, () -> storage.delete("employee-photos/../x.png"));
        storage.delete(null);
        verifyNoInteractions(s3);
    }

    @Test
    void propertiesFailFastWithoutBucket() {
        var error = assertThrows(IllegalStateException.class,
                () -> new S3StorageProperties("", "employee-photos/", "us-east-1", null));
        assertTrue(error.getMessage().contains("APP_STORAGE_S3_BUCKET"));
    }
}
