--liquibase formatted sql
--changeset xtirela:005-remove-redundant-keycloak-column

ALTER TABLE participants DROP COLUMN IF EXISTS keycloakid;

--rollback ALTER TABLE participants ADD COLUMN keycloakid TEXT;