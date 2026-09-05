package dev.zorionten.portfolio.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactIntentRepository extends JpaRepository<ContactIntent, UUID> {

	List<ContactIntent> findAllByOrderByCreatedAtDesc();

	List<ContactIntent> findAllBySessionIdOrderByCreatedAtDesc(UUID sessionId);

	ContactIntent findTopByOrderByCreatedAtDesc();
}