--liquibase formatted sql
--changeset xtirela:012-alter-event-registrations

-- имя колонки статуса в соответствии с сущностью EventRegistration.eventRegistrationStatus
ALTER TABLE event_registrations RENAME COLUMN event_reg_request_status TO event_registration_status;

-- ограничение статуса в соответствии с enum EventRegistrationStatus
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS event_registration_event_reg_request_status_check;
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS event_reg_request_status_check;
ALTER TABLE event_registrations ADD CONSTRAINT chk_event_registration_status
    CHECK (event_registration_status IN ('ACCEPTED', 'PENDING', 'DENIED', 'CANCELLED', 'NOT_FOUND', 'DEPRECATED', 'WAITING'));

-- недостающая колонка сущности EventRegistration
ALTER TABLE event_registrations ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- явные внешние ключи (после переименования таблиц)
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS fk_registration_event;
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS fk_registration_participant;
ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id) REFERENCES events (id);
ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_participant FOREIGN KEY (participant_id) REFERENCES participants (id);

--rollback ALTER TABLE event_registrations DROP CONSTRAINT fk_event_registration_event;
--rollback ALTER TABLE event_registrations DROP CONSTRAINT fk_event_registration_participant;
--rollback ALTER TABLE event_registrations DROP COLUMN updated_at;
--rollback ALTER TABLE event_registrations RENAME COLUMN event_registration_status TO event_reg_request_status;
