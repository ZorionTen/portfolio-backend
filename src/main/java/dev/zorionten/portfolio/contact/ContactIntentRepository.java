package dev.zorionten.portfolio.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ContactIntentRepository extends JpaRepository<ContactIntent, UUID> {
}
