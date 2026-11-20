package ch.bbw.trips.SpringBootBusinessTripsv2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.bbw.trips.storage.LocalPhotoStorage;
import ch.bbw.trips.storage.PhotoStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Default profile: H2 + local storage, so the context starts without MariaDB, RDS or S3. */
@SpringBootTest(properties = "app.storage.location=${java.io.tmpdir}/biztrips-test-uploads")
class SpringBootBusinessTripsv2ApplicationTests {

	@Autowired
	PhotoStorage storage;

	@Test
	void contextLoadsWithLocalStorageByDefault() {
		assertTrue(storage instanceof LocalPhotoStorage, storage.describe());
	}
}
