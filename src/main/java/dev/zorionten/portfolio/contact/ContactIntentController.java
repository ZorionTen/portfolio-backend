package dev.zorionten.portfolio.contact;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/contact-intents")
class ContactIntentController {

	private final ContactIntentRepository repository;

	ContactIntentController(ContactIntentRepository repository) {
		this.repository = repository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ContactIntentResponse create(@Valid @RequestBody ContactIntentRequest request) {
		ContactIntent contactIntent = repository.save(
				new ContactIntent(request.name(), request.companyName(), request.email(), request.sessionId())
		);

		return new ContactIntentResponse(contactIntent.getId());
	}

	record ContactIntentRequest(
			@Size(max = 120) String name,
			@Size(max = 120) String companyName,
			@Email @Size(max = 254) String email,
			UUID sessionId
	) {
	}

	record ContactIntentResponse(UUID id) {
	}
}