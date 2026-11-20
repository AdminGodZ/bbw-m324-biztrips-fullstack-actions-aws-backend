package ch.bbw.trips.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StorageExceptionHandlerTest {
    @Test
    void storageFailureIs503WithoutInternalDetails() {
        var problem = new StorageExceptionHandler().storageUnavailable(new StorageUnavailableException(
                "ACCESS_DENIED", "PutObject on s3://secret-bucket/employee-photos/ denied", null));
        assertEquals(503, problem.getStatus());
        assertEquals("ACCESS_DENIED", problem.getProperties().get("reason"));
        assertFalse(problem.getDetail().contains("secret-bucket"), "bucket name must not leak to clients");
    }
}
