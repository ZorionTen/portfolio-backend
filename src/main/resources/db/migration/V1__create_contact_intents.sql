CREATE TABLE contact_intents (
    id UUID PRIMARY KEY,
    company_name VARCHAR(120),
    application_email VARCHAR(254),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX contact_intents_created_at_idx ON contact_intents (created_at DESC);
