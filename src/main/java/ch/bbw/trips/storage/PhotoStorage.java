package ch.bbw.trips.storage;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage for employee profile photos.
 *
 * <p>Implementations are selected with {@code app.storage.type}:
 * {@code local} (RefCard04, file system / Docker volume) or {@code s3} (RefCard05, Amazon S3).
 * The database only stores the generated file name, never a path or a URL.
 */
public interface PhotoStorage {

    /** Validates the upload and stores it under a new random name. Returns that name. */
    String store(MultipartFile file) throws IOException;

    Resource load(String filename) throws IOException;

    /** Deletes a stored photo. {@code null} is ignored. */
    void delete(String filename) throws IOException;

    default String contentType(String filename) {
        return PhotoValidator.contentType(filename);
    }

    /** Short description for logs and the info endpoint, e.g. "s3://bucket/prefix". */
    String describe();
}
