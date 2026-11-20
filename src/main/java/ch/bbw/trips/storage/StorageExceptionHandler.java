package ch.bbw.trips.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns storage failures into an RFC 9457 problem response without leaking bucket names or AWS details. */
@RestControllerAdvice
public class StorageExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(StorageExceptionHandler.class);

    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail storageUnavailable(StorageUnavailableException ex) {
        log.error("Photo storage unavailable ({}): {}", ex.getReason(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Der Bilder-Speicher ist momentan nicht verfügbar. Bitte später erneut versuchen.");
        problem.setTitle("Photo storage unavailable");
        problem.setProperty("reason", ex.getReason());
        return problem;
    }
}
