package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private final Path root;

    public FileStorageService(@Value("${app.storage.location}") String location) throws IOException {
        root = Paths.get(location).toAbsolutePath().normalize();
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Storage directory cannot be a symlink");
    }

    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        byte[] bytes;
        try (var in = file.getInputStream()) { bytes = in.readNBytes((int) MAX_BYTES + 1); }
        if (bytes.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        String type = file.getContentType();
        String extension;
        if ("image/png".equals(type) && bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                new byte[]{(byte)137,80,78,71,13,10,26,10})) extension = ".png";
        else if ("image/jpeg".equals(type) && bytes.length >= 3 && (bytes[0]&255)==255
                && (bytes[1]&255)==216 && (bytes[2]&255)==255) extension = ".jpg";
        else if ("image/webp".equals(type) && bytes.length >= 12
                && new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) extension = ".webp";
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected PNG, JPEG or WebP image data");
        String filename = UUID.randomUUID() + extension;
        Path target = safePath(filename);
        try { Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE); }
        catch (IOException ex) { Files.deleteIfExists(target); throw ex; }
        return filename;
    }

    private Path safePath(String filename) {
        if (filename == null || !filename.matches("[a-f0-9-]{36}\\.(png|jpg|webp)"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage reference");
        Path target = root.resolve(filename).normalize();
        if (!root.equals(target.getParent()) || Files.isSymbolicLink(target))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        return target;
    }

    public Resource load(String filename) throws IOException {
        Path path = safePath(filename);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        try (var channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             var in = java.nio.channels.Channels.newInputStream(channel)) {
            byte[] bytes = in.readNBytes((int)MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Stored photo exceeds limit");
            return new ByteArrayResource(bytes);
        }
    }

    public void delete(String filename) throws IOException {
        if (filename != null) Files.deleteIfExists(safePath(filename));
    }

    public String contentType(String filename) {
        safePath(filename);
        return filename.endsWith(".png") ? "image/png" : filename.endsWith(".webp") ? "image/webp" : "image/jpeg";
    }
}
