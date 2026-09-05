package dev.zorionten.portfolio.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_intents")
public class ContactIntent {

	@Id
	private UUID id;

	@Column(name = "name", length = 120)
	private String name;

	@Column(name = "company_name", length = 120)
	private String companyName;

	@Column(name = "email", length = 254)
	private String email;

	@Column(name = "session_id")
	private UUID sessionId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ContactIntent() {
	}

	public ContactIntent(String name, String companyName, String email, UUID sessionId) {
		this.id = UUID.randomUUID();
		this.name = normalize(name);
		this.companyName = normalize(companyName);
		this.email = normalize(email);
		this.sessionId = sessionId;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getEmail() {
		return email;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}