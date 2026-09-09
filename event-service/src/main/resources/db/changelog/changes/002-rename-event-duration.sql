--liquibase formatted sql
--changeset xtirela:002-rename-event-duration

ALTER TABLE events RENAME COLUMN event_duration TO event_duration_minutes;

--rollback ALTER TABLE events RENAME COLUMN event_duration_minutes TO event_duration;
