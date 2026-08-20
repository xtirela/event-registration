--liquibase formatted sql
--changeset xtirela:004-add-registration-created-at
ALTER TABLE event_registration ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

--rollback ALTER TABLE event_registration DROP COLUMN created_at;