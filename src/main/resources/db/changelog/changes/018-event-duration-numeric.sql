--liquibase formatted sql

--changeset xtirela:018-event-duration-numeric

ALTER TABLE events
ALTER COLUMN event_duration TYPE NUMERIC(21,0)
USING event_duration::NUMERIC(21,0);

-- rollback ALTER TABLE events ALTER COLUMN event_duration TYPE BIGINT USING event_duration::BIGINT;