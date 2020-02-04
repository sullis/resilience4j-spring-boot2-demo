package io.github.robwin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = Application.class)
@DirtiesContext
public class FutureRetryTest {

	private static final String BACKEND_A = "backendA";
	private static final String BACKEND_B = "backendB";

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void backendAshouldRetryThreeTimes() {
		// When
		produceFailure(BACKEND_A);

		checkMetrics("failed_with_retry", BACKEND_A, "1.0");
	}

	@Test
	public void backendBshouldRetryThreeTimes() {
		// When
		produceFailure(BACKEND_B);

		checkMetrics("failed_with_retry", BACKEND_B, "1.0");
	}

	@Test
	public void backendAshouldSucceedWithoutRetry() {
		produceSuccess(BACKEND_A);

		checkMetrics("successful_without_retry", BACKEND_A, "1.0");
	}

	@Test
	public void backendBshouldSucceedWithoutRetry() {
		produceSuccess(BACKEND_B);

		checkMetrics("successful_without_retry", BACKEND_B, "1.0");
	}

	private void checkMetrics(String kind, String backend, String count) {
		ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
		assertThat(metricsResponse.getBody()).isNotNull();
		String response = metricsResponse.getBody();
		assertThat(response).contains("resilience4j_retry_calls_total{application=\"resilience4j-demo\",kind=\"" + kind + "\",name=\"" +  backend + "\",} " + count);
	}

	private void produceFailure(String backend) {
		ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/futureFailure", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void produceSuccess(String backend) {
		ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/futureSuccess", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


}
