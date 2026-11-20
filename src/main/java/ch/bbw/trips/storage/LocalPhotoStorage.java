package ch.bbw.trips.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** RefCard04 storage: photos on the local file system or a Docker volume. Default for local development. */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalPhotoStorage implements PhotoStorage {
    private final Path root;

    public LocalPhotoStorage(@Value("${app.storage.location}") String location) throws IOException {
        root = Paths.get(location).toAbsolutePath().normalize();
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Storage directory cannot be a symlink");
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        var photo = PhotoValidator.validate(file);
        Path target = safePath(photo.filename());
        try {
            Files.write(target, photo.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
        return photo.filename();
    }

    private Path safePath(String filename) {
        PhotoValidator.requireValidName(filename);
        Path target = root.resolve(filename).normalize();
        if (!root.equals(target.getParent()) || Files.isSymbolicLink(target))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        return target;
    }

    @Override
    public Resource load(String filename) throws IOException {
        Path path = safePath(filename);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        try (var channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             var in = java.nio.channels.Channels.newInputStream(channel)) {
            byte[] bytes = in.readNBytes((int) PhotoValidator.MAX_BYTES + 1);
            if (bytes.length > PhotoValidator.MAX_BYTES) throw new IOException("Stored photo exceeds limit");
            return new ByteArrayResource(bytes);
        }
    }

    @Override
    public void delete(String filename) throws IOException {
        if (filename != null) Files.deleteIfExists(safePath(filename));
    }

    @Override
    public String describe() {
        return "local:" + root;
    }
}
