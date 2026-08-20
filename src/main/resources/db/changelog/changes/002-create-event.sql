--liquibase formatted sql
--changeset xtirela:002-create-event

CREATE TABLE event (
                       id                       SERIAL PRIMARY KEY,
                       event_name               TEXT NOT NULL,
                       location                 TEXT NOT NULL,
                       event_date               TIMESTAMPTZ  NOT NULL,
                       event_duration           INTERVAL     NOT NULL,
                       age_required             INTEGER      NOT NULL CHECK (age_required >= 0 AND age_required <= 150),
                       event_gender_requirement VARCHAR(20)  NOT NULL CHECK (event_gender_requirement IN ('MALE_ONLY', 'FEMALE_ONLY', 'NONE')),
                       current_participant_amount INTEGER    NOT NULL DEFAULT 0,
                       max_participant_amount   INTEGER      NOT NULL CHECK (max_participant_amount > 0),
                       event_status             VARCHAR(20)  NOT NULL CHECK (event_status IN ('ONGOING', 'CANCELLED', 'ENDED', 'PLANNED')),
                       event_registration_status VARCHAR(20) NOT NULL CHECK (event_registration_status IN ('ALL_RESERVED', 'RESERVATIONS_CLOSED', 'RESERVATIONS_OPEN')),
                       created_at               TIMESTAMPTZ  NOT NULL,
                       CONSTRAINT chk_event_capacity CHECK (current_participant_amount <= max_participant_amount)
);

ALTER TABLE event ADD CONSTRAINT uk_event_name UNIQUE (event_name);

--rollback DROP TABLE event;