package dev.zorionten.portfolio.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat-messages")
class ChatHistoryController {

	private final ChatMessageRepository repository;

	ChatHistoryController(ChatMessageRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	List<ChatMessageResponse> list(@RequestParam UUID sessionId) {
		List<ChatMessage> messages = new ArrayList<>(
				repository.findTop50BySessionIdOrderByCreatedAtDesc(sessionId)
		);
		Collections.reverse(messages);
		return messages.stream().map(ChatHistoryController::toResponse).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	ChatMessageResponse create(@Valid @RequestBody ChatMessageRequest request) {
		ChatMessage saved = repository.save(new ChatMessage(
				request.sessionId(),
				request.role(),
				request.content(),
				request.sources()
		));
		List<ChatMessage> messages = repository.findAllBySessionIdOrderByCreatedAtDesc(request.sessionId());
		if (messages.size() > 50) {
			repository.deleteAllInBatch(messages.subList(50, messages.size()));
		}
		return toResponse(saved);
	}

	private static ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
				message.getId(),
				message.getRole().name().toLowerCase(),
				message.getContent(),
				message.getSources(),
				message.getCreatedAt()
		);
	}

	record ChatMessageRequest(
			@NotNull UUID sessionId,
			@NotNull ChatRole role,
			@NotBlank @Size(max = 5000) String content,
			@Size(max = 20) List<@Size(max = 120) String> sources
	) {
	}

	record ChatMessageResponse(
			UUID id,
			String role,
			String content,
			List<String> sources,
			Instant createdAt
	) {
	}
}
