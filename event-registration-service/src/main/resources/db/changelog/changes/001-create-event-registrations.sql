    --liquibase formatted sql

--changeset xtirela:001-create-event-registrations
CREATE TABLE event_registrations (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_registration_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    description              VARCHAR(500),
    event_id                 BIGINT NOT NULL,
    participant_id           BIGINT NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_registrations_event_id ON event_registrations(event_id);

--rollback DROP TABLE event_registrations;