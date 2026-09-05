package dev.zorionten.portfolio.admin;

import dev.zorionten.portfolio.chat.ChatMessage;
import dev.zorionten.portfolio.chat.ChatMessageRepository;
import dev.zorionten.portfolio.chat.ChatRole;
import dev.zorionten.portfolio.contact.ContactIntent;
import dev.zorionten.portfolio.contact.ContactIntentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTests {

	private final ContactIntentRepository contactRepository = mock(ContactIntentRepository.class);
	private final ChatMessageRepository chatRepository = mock(ChatMessageRepository.class);
	private final AdminController controller = new AdminController(contactRepository, chatRepository);

	@Test
	void listsSessionsWithLinkedEmails() {
		UUID sessionId = UUID.randomUUID();
		ChatMessageRepository.SessionSummary summary = mock(ChatMessageRepository.SessionSummary.class);
		when(summary.getSessionId()).thenReturn(sessionId);
		when(summary.getMessageCount()).thenReturn(3L);
		when(summary.getFirstActivity()).thenReturn(Instant.parse("2026-09-01T10:00:00Z"));
		when(summary.getLastActivity()).thenReturn(Instant.parse("2026-09-01T10:05:00Z"));
		when(chatRepository.findSessionSummaries()).thenReturn(List.of(summary));
		when(contactRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
				new ContactIntent("Alice", "Acme", "alice@acme.com", sessionId)
		));

		List<AdminController.SessionResponse> sessions = controller.sessions();

		assertThat(sessions).hasSize(1);
		assertThat(sessions.get(0).sessionId()).isEqualTo(sessionId);
		assertThat(sessions.get(0).messageCount()).isEqualTo(3);
		assertThat(sessions.get(0).emails()).containsExactly("alice@acme.com");
	}

	@Test
	void returnsSessionDetailWithChronologicalTranscript() {
		UUID sessionId = UUID.randomUUID();
		ChatMessage first = new ChatMessage(sessionId, ChatRole.USER, "Hello", List.of());
		ChatMessage second = new ChatMessage(sessionId, ChatRole.ASSISTANT, "Hi there", List.of("source-a"));
		when(chatRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(first, second));
		when(contactRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionId)).thenReturn(List.of());

		AdminController.SessionDetailResponse detail = controller.session(sessionId);

		assertThat(detail.messageCount()).isEqualTo(2);
		assertThat(detail.messages()).hasSize(2);
		assertThat(detail.messages().get(0).role()).isEqualTo("user");
		assertThat(detail.messages().get(1).role()).isEqualTo("assistant");
		assertThat(detail.messages().get(1).sources()).containsExactly("source-a");
	}

	@Test
	void returnsNotFoundForUnknownSession() {
		UUID sessionId = UUID.randomUUID();
		when(chatRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of());

		assertThatThrownBy(() -> controller.session(sessionId))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("404");
	}

	@Test
	void listsContactIntentsNewestFirst() {
		ContactIntent contact = new ContactIntent("Alice", "Acme", "alice@acme.com", UUID.randomUUID());
		when(contactRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(contact));

		List<AdminController.ContactIntentResponse> contacts = controller.contactIntents();

		assertThat(contacts).hasSize(1);
		assertThat(contacts.get(0).name()).isEqualTo("Alice");
		assertThat(contacts.get(0).companyName()).isEqualTo("Acme");
		assertThat(contacts.get(0).email()).isEqualTo("alice@acme.com");
		assertThat(contacts.get(0).sessionId()).isNotNull();
	}

	@Test
	void returnsContactIntentById() {
		UUID id = UUID.randomUUID();
		ContactIntent contact = new ContactIntent("Bob", null, "bob@example.com", null);
		when(contactRepository.findById(id)).thenReturn(Optional.of(contact));

		AdminController.ContactIntentResponse response = controller.contactIntent(id);

		assertThat(response.name()).isEqualTo("Bob");
		assertThat(response.companyName()).isNull();
		assertThat(response.sessionId()).isNull();
	}

	@Test
	void returnsNotFoundForUnknownContactIntent() {
		UUID id = UUID.randomUUID();
		when(contactRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> controller.contactIntent(id))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("404");
	}

	@Test
	void returnsStatsWithMostRecentContact() {
		ContactIntent mostRecent = new ContactIntent("Alice", "Acme", "alice@acme.com", UUID.randomUUID());
		when(contactRepository.findTopByOrderByCreatedAtDesc()).thenReturn(mostRecent);
		when(contactRepository.count()).thenReturn(5L);
		when(chatRepository.countDistinctSessions()).thenReturn(3L);

		AdminController.StatsResponse stats = controller.stats();

		assertThat(stats.mostRecentContact().name()).isEqualTo("Alice");
		assertThat(stats.totalContactIntents()).isEqualTo(5);
		assertThat(stats.totalSessions()).isEqualTo(3);
	}
}