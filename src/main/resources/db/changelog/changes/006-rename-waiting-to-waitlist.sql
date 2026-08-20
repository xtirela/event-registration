--liquibase formatted sql

--changeset xtirela:006-rename-waiting-to-waitlist

-- Удаляем ограничение с WAITING
ALTER TABLE event DROP CONSTRAINT chk_event_registration_status;

-- Добавляем с WAITLIST
ALTER TABLE event ADD CONSTRAINT chk_event_registration_status
    CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN', 'WAITLIST'));

--rollback ALTER TABLE event DROP CONSTRAINT chk_event_registration_status;
--rollback ALTER TABLE event ADD CONSTRAINT chk_event_registration_status CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN', 'WAITING'));