package dev.zorionten.portfolio.admin;

import dev.zorionten.portfolio.chat.ChatMessage;
import dev.zorionten.portfolio.chat.ChatMessageRepository;
import dev.zorionten.portfolio.contact.ContactIntent;
import dev.zorionten.portfolio.contact.ContactIntentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
class AdminController {

	private final ContactIntentRepository contactRepository;
	private final ChatMessageRepository chatRepository;

	AdminController(ContactIntentRepository contactRepository, ChatMessageRepository chatRepository) {
		this.contactRepository = contactRepository;
		this.chatRepository = chatRepository;
	}

	@GetMapping("/sessions")
	List<SessionResponse> sessions() {
		Map<UUID, List<String>> emailsBySession = emailsBySession();
		return chatRepository.findSessionSummaries().stream()
				.map(summary -> new SessionResponse(
						summary.getSessionId(),
						summary.getMessageCount(),
						summary.getFirstActivity(),
						summary.getLastActivity(),
						emailsBySession.getOrDefault(summary.getSessionId(), List.of())
				))
				.toList();
	}

	@GetMapping("/sessions/{sessionId}")
	SessionDetailResponse session(@PathVariable UUID sessionId) {
		List<ChatMessage> messages = chatRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
		if (messages.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
		}
		List<String> emails = contactRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionId).stream()
				.map(ContactIntent::getEmail)
				.filter(email -> email != null)
				.toList();
		return new SessionDetailResponse(
				sessionId,
				messages.size(),
				messages.get(0).getCreatedAt(),
				messages.get(messages.size() - 1).getCreatedAt(),
				emails,
				messages.stream().map(AdminController::toChatMessageResponse).toList()
		);
	}

	@GetMapping("/contact-intents")
	List<ContactIntentResponse> contactIntents() {
		return contactRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(AdminController::toContactIntentResponse)
				.toList();
	}

	@GetMapping("/contact-intents/{id}")
	ContactIntentResponse contactIntent(@PathVariable UUID id) {
		ContactIntent contactIntent = contactRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact intent not found"));
		return toContactIntentResponse(contactIntent);
	}

	@GetMapping("/contact-intents/session/{sessionId}")
	List<ContactIntentResponse> contactIntentsBySession(@PathVariable UUID sessionId) {
		return contactRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionId).stream()
				.map(AdminController::toContactIntentResponse)
				.toList();
	}

	@GetMapping("/chats/{sessionId}")
	List<ChatMessageResponse> chats(@PathVariable UUID sessionId) {
		List<ChatMessage> messages = chatRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
		if (messages.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
		}
		return messages.stream().map(AdminController::toChatMessageResponse).toList();
	}

	@GetMapping("/stats")
	StatsResponse stats() {
		ContactIntent mostRecent = contactRepository.findTopByOrderByCreatedAtDesc();
		return new StatsResponse(
				mostRecent == null ? null : toContactIntentResponse(mostRecent),
				contactRepository.count(),
				chatRepository.countDistinctSessions()
		);
	}

	private Map<UUID, List<String>> emailsBySession() {
		return contactRepository.findAllByOrderByCreatedAtDesc().stream()
				.filter(contact -> contact.getSessionId() != null && contact.getEmail() != null)
				.collect(Collectors.groupingBy(
						ContactIntent::getSessionId,
						Collectors.mapping(ContactIntent::getEmail, Collectors.toList())
				));
	}

	private static ContactIntentResponse toContactIntentResponse(ContactIntent contactIntent) {
		return new ContactIntentResponse(
				contactIntent.getId(),
				contactIntent.getName(),
				contactIntent.getCompanyName(),
				contactIntent.getEmail(),
				contactIntent.getSessionId(),
				contactIntent.getCreatedAt()
		);
	}

	private static ChatMessageResponse toChatMessageResponse(ChatMessage message) {
		return new ChatMessageResponse(
				message.getId(),
				message.getRole().name().toLowerCase(),
				message.getContent(),
				message.getSources(),
				message.getCreatedAt()
		);
	}

	record SessionResponse(
			UUID sessionId,
			long messageCount,
			Instant firstActivity,
			Instant lastActivity,
			List<String> emails
	) {
	}

	record SessionDetailResponse(
			UUID sessionId,
			long messageCount,
			Instant firstActivity,
			Instant lastActivity,
			List<String> emails,
			List<ChatMessageResponse> messages
	) {
	}

	record ContactIntentResponse(
			UUID id,
			String name,
			String companyName,
			String email,
			UUID sessionId,
			Instant createdAt
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

	record StatsResponse(
			ContactIntentResponse mostRecentContact,
			long totalContactIntents,
			long totalSessions
	) {
	}
}