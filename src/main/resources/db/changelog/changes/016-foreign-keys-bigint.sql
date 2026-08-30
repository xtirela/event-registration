--liquibase formatted sql
--changeset xtirela:016-foreign-keys-bigint

-- Снять внешние ключи, перевести FK-колонки в BIGINT и восстановить ключи
-- (PK-колонки id уже приведены к BIGINT в changeset 013-fix-id-types)

ALTER TABLE participants DROP CONSTRAINT IF EXISTS fk_participant_user;
ALTER TABLE events DROP CONSTRAINT IF EXISTS fk_event_organizer;
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS fk_event_registration_event;
ALTER TABLE event_registrations DROP CONSTRAINT IF EXISTS fk_event_registration_participant;

ALTER TABLE events ALTER COLUMN organizer_id TYPE BIGINT;
ALTER TABLE participants ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE event_registrations ALTER COLUMN event_id TYPE BIGINT;
ALTER TABLE event_registrations ALTER COLUMN participant_id TYPE BIGINT;

ALTER TABLE participants ADD CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE events ADD CONSTRAINT fk_event_organizer FOREIGN KEY (organizer_id) REFERENCES users (id);
ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id) REFERENCES events (id);
ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_participant FOREIGN KEY (participant_id) REFERENCES participants (id);

--rollback ALTER TABLE event_registrations DROP CONSTRAINT fk_event_registration_participant;
--rollback ALTER TABLE event_registrations DROP CONSTRAINT fk_event_registration_event;
--rollback ALTER TABLE events DROP CONSTRAINT fk_event_organizer;
--rollback ALTER TABLE participants DROP CONSTRAINT fk_participant_user;
--rollback ALTER TABLE events ALTER COLUMN organizer_id TYPE INTEGER;
--rollback ALTER TABLE participants ALTER COLUMN user_id TYPE INTEGER;
--rollback ALTER TABLE event_registrations ALTER COLUMN event_id TYPE INTEGER;
--rollback ALTER TABLE event_registrations ALTER COLUMN participant_id TYPE INTEGER;
--rollback ALTER TABLE participants ADD CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id);
--rollback ALTER TABLE events ADD CONSTRAINT fk_event_organizer FOREIGN KEY (organizer_id) REFERENCES users (id);
--rollback ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id) REFERENCES events (id);
--rollback ALTER TABLE event_registrations ADD CONSTRAINT fk_event_registration_participant FOREIGN KEY (participant_id) REFERENCES participants (id);
