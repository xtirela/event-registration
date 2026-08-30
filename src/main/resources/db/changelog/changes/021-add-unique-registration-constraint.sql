--liquibase formatted sql

--changeset xtirela:021-add-unique-registration-constraint

ALTER TABLE event_registrations ADD CONSTRAINT uk_registration_event_participant UNIQUE (event_id, participant_id);
ALTER TABLE events ALTER COLUMN organizer_id SET NOT NULL;

--rollback ALTER TABLE event_registrations DROP CONSTRAINT uk_registration_event_participant;