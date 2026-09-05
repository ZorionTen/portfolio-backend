ALTER TABLE contact_intents RENAME COLUMN visitor_name TO name;
ALTER TABLE contact_intents ADD COLUMN session_id UUID;
ALTER TABLE contact_intents RENAME COLUMN application_email TO email;

CREATE INDEX idx_contact_intents_session ON contact_intents (session_id);