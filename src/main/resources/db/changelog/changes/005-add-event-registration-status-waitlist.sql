--liquibase formatted sql
--changeset xtirela:005-add-event-registration-status-waitlist
ALTER TABLE event DROP CONSTRAINT event_event_registration_status_check;

ALTER TABLE event ADD CONSTRAINT chk_event_registration_status
    CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN', 'WAITING'));

--rollback ALTER TABLE event DROP CONSTRAINT chk_event_registration_status;
--rollback ALTER TABLE event ADD CONSTRAINT event_event_registration_status_check CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN'));