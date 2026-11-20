package ch.bbw.trips.storage;

/**
 * The photo storage cannot be used (missing permissions, missing bucket, network problem).
 * Mapped to HTTP 503 so clients see a temporary server-side problem instead of a generic 500.
 */
public class StorageUnavailableException extends RuntimeException {
    private final String reason;

    public StorageUnavailableException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /** Machine-readable reason, e.g. {@code ACCESS_DENIED}, {@code BUCKET_NOT_FOUND}, {@code UNREACHABLE}. */
    public String getReason() {
        return reason;
    }
}
