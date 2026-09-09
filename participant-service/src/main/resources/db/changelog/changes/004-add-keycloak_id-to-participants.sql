--liquibase formatted sql
--changeset xtirela:004-add-keycloak_id-to-participants.sql

-- связь участник -> пользователь (Participant.user -> user_id, @OneToOne)
ALTER TABLE participants ADD COLUMN keycloak_id TEXT UNIQUE;


--rollback ALTER TABLE participants DROP COLUMN keycloak_id;
