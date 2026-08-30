--liquibase formatted sql
--changeset xtirela:009-rename-tables-to-entity-names

ALTER TABLE participant RENAME TO participants;
ALTER TABLE event RENAME TO events;
ALTER TABLE event_registration RENAME TO event_registrations;

-- FK constraints автоматически пересоздаются PostgreSQL на новые имена таблиц

--rollback ALTER TABLE participants RENAME TO participant;
--rollback ALTER TABLE events RENAME TO event;
--rollback ALTER TABLE event_registrations RENAME TO event_registration;
