--liquibase formatted sql

--changeset xtirela:004-organizer-keycloak-id-text-unique
DROP INDEX IF EXISTS idx_events_organizer;
ALTER TABLE events ALTER COLUMN organizer_keycloak_id TYPE TEXT;
ALTER TABLE events ADD CONSTRAINT uq_events_organizer_keycloak_id UNIQUE (organizer_keycloak_id);

--rollback ALTER TABLE events DROP CONSTRAINT uq_events_organizer_keycloak_id;
--rollback ALTER TABLE events ALTER COLUMN organizer_keycloak_id TYPE VARCHAR(36);
--rollback CREATE INDEX idx_events_organizer ON events(organizer_keycloak_id);
