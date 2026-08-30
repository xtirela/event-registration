--liquibase formatted sql

--changeset xtirela:019-replace-var-with-text
-- events
ALTER TABLE events ALTER COLUMN event_gender_requirement TYPE TEXT USING event_gender_requirement::TEXT;
ALTER TABLE events ALTER COLUMN event_status TYPE TEXT USING event_status::TEXT;
ALTER TABLE events ALTER COLUMN event_reservation_status TYPE TEXT USING event_reservation_status::TEXT;

-- event_registrations
ALTER TABLE event_registrations ALTER COLUMN event_registration_status TYPE TEXT USING event_registration_status::TEXT;

-- users (если role enum)
ALTER TABLE users ALTER COLUMN role TYPE TEXT USING role::TEXT;

-- participants (если gender enum)
ALTER TABLE participants ALTER COLUMN participant_gender TYPE TEXT USING participant_gender::TEXT;

-- rollback ALTER TABLE events ALTER COLUMN event_gender_requirement TYPE VARCHAR(20) USING event_gender_requirement::VARCHAR(20);
-- rollback ALTER TABLE events ALTER COLUMN event_status TYPE VARCHAR(20) USING event_status::VARCHAR(20);
-- rollback ALTER TABLE events ALTER COLUMN event_reservation_status TYPE VARCHAR(20) USING event_reservation_status::VARCHAR(20);
-- rollback ALTER TABLE event_registrations ALTER COLUMN event_registration_status TYPE VARCHAR(20) USING event_registration_status::VARCHAR(20);
-- rollback ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::VARCHAR(20);
-- rollback ALTER TABLE participants ALTER COLUMN participant_gender TYPE VARCHAR(20) USING participant_gender::VARCHAR(20);