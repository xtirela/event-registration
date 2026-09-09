--liquibase formatted sql
--changeset xtirela:003-fix-id-types

ALTER TABLE participants
ALTER COLUMN id TYPE BIGINT;


-- rollback ALTER TABLE participants ALTER COLUMN id TYPE INTEGER;
