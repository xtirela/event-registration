--liquibase formatted sql
--changeset xtirela:017-event-change-duration-type

ALTER TABLE events ALTER COLUMN event_duration TYPE BIGINT
USING EXTRACT(EPOCH FROM event_duration) * 1000000000;

-- rollback ALTER TABLE events ALTER COLUMN event_duration TYPE INTERVAL USING event_duration * INTERVAL '1 nanosecond';