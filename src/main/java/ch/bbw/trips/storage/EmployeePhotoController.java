package ch.bbw.trips.storage;

import ch.bbw.trips.repo.Employee;
import ch.bbw.trips.repo.EmployeeRepository;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/employees/{id}/photo")
public class EmployeePhotoController {
    private static final Logger log = LoggerFactory.getLogger(EmployeePhotoController.class);
    private final EmployeeRepository employees;
    private final PhotoStorage storage;

    public EmployeePhotoController(EmployeeRepository employees, PhotoStorage storage) {
        this.employees = employees;
        this.storage = storage;
    }

    private Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Employee employee = employee(id);
        String old = employee.getProfilePictureFilename();
        String filename = storage.store(file);
        try {
            employee.setProfilePictureFilename(filename);
            employees.save(employee);
        } catch (RuntimeException ex) {
            storage.delete(filename);
            throw ex;
        }
        try { storage.delete(old); }
        catch (IOException | RuntimeException ex) { log.warn("Previous photo cleanup failed for employee {}", id); }
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        String filename = employee(id).getProfilePictureFilename();
        if (filename == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(storage.contentType(filename)))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(storage.load(filename));
    }
}
