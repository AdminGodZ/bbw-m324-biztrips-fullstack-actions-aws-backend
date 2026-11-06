package ch.bbw.trips.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path root;
    static byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=");
    @Test void survivesNewServiceAndIgnoresOriginalName() throws Exception {
        var storage = new FileStorageService(root.toString());
        String one = storage.store(new MockMultipartFile("file", "../../photo.png", "image/png", png));
        String two = storage.store(new MockMultipartFile("file", "../../photo.png", "image/png", png));
        assertNotEquals(one,two);
        assertArrayEquals(png,new FileStorageService(root.toString()).load(one).getContentAsByteArray());
        assertEquals("image/png", storage.contentType(one));
    }
    @Test void rejectsEmptySpoofedAndOversizedFiles() throws Exception {
        var storage = new FileStorageService(root.toString());
        assertThrows(ResponseStatusException.class,()->storage.store(new MockMultipartFile("file",new byte[0])));
        assertThrows(ResponseStatusException.class,()->storage.store(new MockMultipartFile("file","x.png","image/png","not a PNG".getBytes())));
        var oversized = new byte[5*1024*1024+1];
        var error = assertThrows(ResponseStatusException.class,()->storage.store(new MockMultipartFile("file","x.png","image/png",oversized)));
        assertEquals(413,error.getStatusCode().value());
    }
    @Test void rejectsTraversalAndSymlinkAndMissingFile() throws Exception {
        var storage = new FileStorageService(root.toString());
        assertThrows(ResponseStatusException.class,()->storage.load("../../etc/passwd"));
        String missing = java.util.UUID.randomUUID()+".png";
        assertEquals(404,assertThrows(ResponseStatusException.class,()->storage.load(missing)).getStatusCode().value());
        Path outside = Files.createTempFile("photo-outside", ".png");
        try {
            Files.createSymbolicLink(root.resolve(missing),outside);
            assertThrows(ResponseStatusException.class,()->storage.load(missing));
        } finally { Files.delete(outside); }
    }
}
