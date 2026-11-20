package ch.bbw.trips.storage.s3;

import ch.bbw.trips.storage.PhotoStorage;
import ch.bbw.trips.storage.PhotoValidator;
import ch.bbw.trips.storage.StorageUnavailableException;
import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * RefCard05 storage: photos as private objects in Amazon S3 under {@code <prefix><uuid>.<ext>}.
 * Objects are never public; the backend streams them to the client.
 */
public class S3PhotoStorage implements PhotoStorage {
    private final S3Client s3;
    private final S3StorageProperties properties;

    public S3PhotoStorage(S3Client s3, S3StorageProperties properties) {
        this.s3 = s3;
        this.properties = properties;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        var photo = PhotoValidator.validate(file);
        call("PutObject", () -> s3.putObject(b -> b.bucket(properties.bucket())
                        .key(properties.key(photo.filename()))
                        .contentType(photo.contentType())
                        .contentLength((long) photo.bytes().length),
                RequestBody.fromBytes(photo.bytes())));
        return photo.filename();
    }

    @Override
    public Resource load(String filename) {
        PhotoValidator.requireValidName(filename);
        try {
            byte[] bytes = call("GetObject", () -> s3.getObjectAsBytes(b -> b.bucket(properties.bucket())
                    .key(properties.key(filename))).asByteArray());
            if (bytes.length > PhotoValidator.MAX_BYTES)
                throw new StorageUnavailableException("INVALID_OBJECT", "Stored photo exceeds limit: " + filename, null);
            return new ByteArrayResource(bytes);
        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
    }

    @Override
    public void delete(String filename) {
        if (filename == null) return;
        PhotoValidator.requireValidName(filename);
        call("DeleteObject", () -> s3.deleteObject(b -> b.bucket(properties.bucket()).key(properties.key(filename))));
    }

    @Override
    public String describe() {
        return "s3://" + properties.bucket() + "/" + properties.prefix();
    }

    /** Executes an S3 call and translates AWS errors into meaningful application errors. */
    private <T> T call(String operation, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (NoSuchKeyException ex) {
            throw ex;
        } catch (S3Exception ex) {
            throw translate(operation, ex);
        } catch (SdkClientException ex) {
            throw new StorageUnavailableException("UNREACHABLE",
                    operation + " failed, S3 not reachable or no credentials available: " + ex.getMessage(), ex);
        }
    }

    StorageUnavailableException translate(String operation, S3Exception ex) {
        String code = ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorCode();
        String target = describe();
        if (ex.statusCode() == 403 || "AccessDenied".equals(code)) {
            return new StorageUnavailableException("ACCESS_DENIED", operation + " on " + target
                    + " denied (HTTP 403). Check the task role / bucket policy (least privilege: "
                    + "s3:GetObject, s3:PutObject, s3:DeleteObject on " + properties.prefix() + "*).", ex);
        }
        if ("NoSuchBucket".equals(code)) {
            return new StorageUnavailableException("BUCKET_NOT_FOUND", operation + ": bucket "
                    + properties.bucket() + " does not exist. Check APP_STORAGE_S3_BUCKET.", ex);
        }
        return new StorageUnavailableException("S3_ERROR", operation + " on " + target + " failed: HTTP "
                + ex.statusCode() + " " + code, ex);
    }
}
