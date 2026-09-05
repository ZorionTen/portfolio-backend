package dev.zorionten.portfolio.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
	List<ChatMessage> findTop50BySessionIdOrderByCreatedAtDesc(UUID sessionId);
	List<ChatMessage> findAllBySessionIdOrderByCreatedAtDesc(UUID sessionId);
	List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

	@Query("""
			SELECT c.sessionId AS sessionId,
			       COUNT(c) AS messageCount,
			       MIN(c.createdAt) AS firstActivity,
			       MAX(c.createdAt) AS lastActivity
			FROM ChatMessage c
			GROUP BY c.sessionId
			ORDER BY MAX(c.createdAt) DESC
			""")
	List<SessionSummary> findSessionSummaries();

	@Query("SELECT COUNT(DISTINCT c.sessionId) FROM ChatMessage c")
	long countDistinctSessions();

	interface SessionSummary {
		UUID getSessionId();
		long getMessageCount();
		Instant getFirstActivity();
		Instant getLastActivity();
	}
}