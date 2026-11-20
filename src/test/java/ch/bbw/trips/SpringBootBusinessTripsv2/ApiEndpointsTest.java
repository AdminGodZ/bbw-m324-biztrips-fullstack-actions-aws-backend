package ch.bbw.trips.SpringBootBusinessTripsv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

/** Calls the real HTTP endpoints (incl. JSON serialization of lazy JPA relations) on a random port. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "app.storage.location=${java.io.tmpdir}/biztrips-api-test-uploads")
class ApiEndpointsTest {

	@Autowired
	Environment environment;

	HttpResponse<String> get(String path) throws Exception {
		String port = environment.getProperty("local.server.port");
		return HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void employeesAreServedWithTheirRelations() throws Exception {
		var response = get("/v1/employees");
		assertEquals(200, response.statusCode(), response.body());
		assertTrue(response.body().contains("Joe Sarnataro"), response.body());
	}

	@Test
	void tripsAndHealthAndVersionAreAvailable() throws Exception {
		assertEquals(200, get("/v1/trips").statusCode());
		var readiness = get("/actuator/health/readiness");
		assertEquals(200, readiness.statusCode(), readiness.body());
		assertTrue(get("/actuator/info").body().contains("\"refcard\":\"RefCard05\""));
	}
}
