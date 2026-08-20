--liquibase formatted sql
--changeset xtirela:007-assert-event-registration-status-waitlist
ALTER TABLE event DROP CONSTRAINT chk_event_registration_status;

ALTER TABLE event ADD CONSTRAINT chk_event_registration_status
    CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN', 'WAITLIST'));

--rollback ALTER TABLE event DROP CONSTRAINT chk_event_registration_status;
--rollback ALTER TABLE event ADD CONSTRAINT chk_event_registration_status CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN'));