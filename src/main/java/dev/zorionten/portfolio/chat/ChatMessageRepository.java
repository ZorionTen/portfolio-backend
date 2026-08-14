package dev.zorionten.portfolio.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
	List<ChatMessage> findTop50BySessionIdOrderByCreatedAtDesc(UUID sessionId);
	List<ChatMessage> findAllBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
