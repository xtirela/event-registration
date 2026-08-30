--liquibase formatted sql
--changeset xtirela:010-alter-events

-- имя колонки статуса в соответствии с сущностью Event.eventReservationStatus -> event_reservation_status
ALTER TABLE events RENAME COLUMN event_registration_status TO event_reservation_status;

-- ограничение статуса в соответствии с enum EventReservationStatus
ALTER TABLE events DROP CONSTRAINT IF EXISTS chk_event_registration_status;
ALTER TABLE events DROP CONSTRAINT IF EXISTS event_event_registration_status_check;
ALTER TABLE events ADD CONSTRAINT chk_event_reservation_status
    CHECK (event_reservation_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN', 'CONFIRMATION_REQUIRED', 'WAITLIST'));

-- недостающие колонки сущности Event
ALTER TABLE events ADD COLUMN event_description TEXT;
ALTER TABLE events ADD COLUMN current_waiting_queue_participant_amount INTEGER NOT NULL DEFAULT 0;
ALTER TABLE events ADD COLUMN confirmation_required BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE events ADD COLUMN waitlist_when_all_reserved BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE events ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- организатор (Event.organizer -> organizer_id) FK -> users
ALTER TABLE events ADD COLUMN organizer_id INTEGER;
ALTER TABLE events ADD CONSTRAINT fk_event_organizer FOREIGN KEY (organizer_id) REFERENCES users (id);

--rollback ALTER TABLE events DROP CONSTRAINT fk_event_organizer;
--rollback ALTER TABLE events DROP COLUMN organizer_id;
--rollback ALTER TABLE events DROP COLUMN updated_at;
--rollback ALTER TABLE events DROP COLUMN waitlist_when_all_reserved;
--rollback ALTER TABLE events DROP COLUMN confirmation_required;
--rollback ALTER TABLE events DROP COLUMN current_waiting_queue_participant_amount;
--rollback ALTER TABLE events DROP COLUMN event_description;
--rollback ALTER TABLE events RENAME COLUMN event_reservation_status TO event_registration_status;
