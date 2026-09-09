--liquibase formatted sql

--changeset xtirela:003-add-unique-event-participant
ALTER TABLE event_registrations
    ADD CONSTRAINT uq_event_registrations_event_participant UNIQUE (event_id, participant_id);

--rollback ALTER TABLE event_registrations DROP CONSTRAINT uq_event_registrations_event_participant;