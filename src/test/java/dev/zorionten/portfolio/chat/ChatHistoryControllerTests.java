package dev.zorionten.portfolio.chat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatHistoryControllerTests {

	@Test
	void removesMessagesBeyondTheLatestFifty() {
		UUID sessionId = UUID.randomUUID();
		ChatMessageRepository repository = mock(ChatMessageRepository.class);
		ChatHistoryController controller = new ChatHistoryController(repository);
		ChatMessage saved = new ChatMessage(sessionId, ChatRole.USER, "Current message", List.of());
		List<ChatMessage> messages = new ArrayList<>();
		for (int index = 0; index < 51; index++) {
			messages.add(new ChatMessage(sessionId, ChatRole.USER, "Message " + index, List.of()));
		}

		when(repository.save(any(ChatMessage.class))).thenReturn(saved);
		when(repository.findAllBySessionIdOrderByCreatedAtDesc(sessionId)).thenReturn(messages);

		controller.create(new ChatHistoryController.ChatMessageRequest(
				sessionId,
				ChatRole.USER,
				"Current message",
				List.of()
		));

		verify(repository).deleteAllInBatch(argThat(expired ->
				StreamSupport.stream(expired.spliterator(), false).count() == 1
		));
	}
}
