package dev.zorionten.portfolio.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_intents")
class ContactIntent {

	@Id
	private UUID id;

	@Column(name = "visitor_name", length = 120)
	private String visitorName;

	@Column(name = "company_name", length = 120)
	private String companyName;

	@Column(name = "application_email", length = 254)
	private String applicationEmail;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ContactIntent() {
	}

	ContactIntent(String visitorName, String companyName, String applicationEmail) {
		this.id = UUID.randomUUID();
		this.visitorName = normalize(visitorName);
		this.companyName = normalize(companyName);
		this.applicationEmail = normalize(applicationEmail);
		this.createdAt = Instant.now();
	}

	UUID getId() {
		return id;
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
