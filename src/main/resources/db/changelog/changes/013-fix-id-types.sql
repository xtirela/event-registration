--liquibase formatted sql
--changeset xtirela:013-fix-id-types

ALTER TABLE event_registrations
ALTER COLUMN id TYPE BIGINT;

ALTER TABLE events
ALTER COLUMN id TYPE BIGINT;

ALTER TABLE participants
ALTER COLUMN id TYPE BIGINT;

ALTER TABLE users
ALTER COLUMN id TYPE BIGINT;

-- rollback ALTER TABLE event_registrations ALTER COLUMN id TYPE INTEGER;
-- rollback ALTER TABLE events ALTER COLUMN id TYPE INTEGER;
-- rollback ALTER TABLE participants ALTER COLUMN id TYPE INTEGER;
-- rollback ALTER TABLE users ALTER COLUMN id TYPE INTEGER;