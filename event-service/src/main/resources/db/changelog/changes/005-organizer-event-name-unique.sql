--liquibase formatted sql

--changeset xtirela:005-organizer-event-name-unique
ALTER TABLE events DROP CONSTRAINT IF EXISTS uq_events_organizer_keycloak_id;
ALTER TABLE events ADD CONSTRAINT uq_events_organizer_event_name UNIQUE (organizer_keycloak_id, event_name);

--rollback ALTER TABLE events DROP CONSTRAINT uq_events_organizer_event_name;
--rollback ALTER TABLE events ADD CONSTRAINT uq_events_organizer_keycloak_id UNIQUE (organizer_keycloak_id);
