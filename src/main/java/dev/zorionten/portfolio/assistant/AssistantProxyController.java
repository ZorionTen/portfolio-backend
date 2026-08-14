package dev.zorionten.portfolio.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/assistant")
class AssistantProxyController {

	private final RestClient assistantClient;

	AssistantProxyController(
			@Value("${ai.service-url:https://portfolio-ai-dla4.onrender.com}") String serviceUrl
	) {
		this.assistantClient = RestClient.builder().baseUrl(serviceUrl).build();
	}

	@GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<String> health() {
		return forward(() -> assistantClient.get().uri("/health").retrieve().toEntity(String.class));
	}

	@PostMapping(
			value = "/chat",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	ResponseEntity<String> chat(@RequestBody String request) {
		return forward(() -> assistantClient.post()
				.uri("/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toEntity(String.class));
	}

	private ResponseEntity<String> forward(Supplier<ResponseEntity<String>> request) {
		try {
			ResponseEntity<String> response = request.get();
			return ResponseEntity.status(response.getStatusCode())
					.contentType(MediaType.APPLICATION_JSON)
					.body(response.getBody());
		} catch (RestClientResponseException exception) {
			return ResponseEntity.status(exception.getStatusCode())
					.contentType(MediaType.APPLICATION_JSON)
					.body(exception.getResponseBodyAsString());
		} catch (RestClientException exception) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"detail\":\"AI service is unavailable\"}");
		}
	}
}
