package dev.zorionten.portfolio.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	private UUID id;

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ChatRole role;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(columnDefinition = "text")
	private String sources;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ChatMessage() {
	}

	public ChatMessage(UUID sessionId, ChatRole role, String content, List<String> sources) {
		this.id = UUID.randomUUID();
		this.sessionId = sessionId;
		this.role = role;
		this.content = content.trim();
		this.sources = sources == null || sources.isEmpty() ? null : String.join("\n", sources);
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public ChatRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public List<String> getSources() {
		return sources == null ? List.of() : sources.lines().toList();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
