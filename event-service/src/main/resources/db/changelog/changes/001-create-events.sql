--liquibase formatted sql
--changeset xtirela:001-create-events

CREATE TABLE events (
                        id                                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        event_name                          VARCHAR(255) NOT NULL,
                        event_description                   VARCHAR(1000),
                        event_date                          TIMESTAMP WITH TIME ZONE NOT NULL,
                        event_duration                      BIGINT NOT NULL,
                        location                            VARCHAR(255) NOT NULL,
                        age_required                        INTEGER NOT NULL DEFAULT 0,
                        event_gender_requirement            VARCHAR(20) NOT NULL CHECK (event_gender_requirement IN ('MALE_ONLY', 'FEMALE_ONLY', 'NONE')),
                        current_participant_amount          INTEGER NOT NULL DEFAULT 0,
                        current_waiting_queue_participant_amount INTEGER NOT NULL DEFAULT 0,
                        max_participant_amount              INTEGER NOT NULL CHECK (max_participant_amount >= 1),
                        event_status                        VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
                        event_reservation_status            VARCHAR(30) NOT NULL DEFAULT 'RESERVATIONS_OPEN',
                        confirmation_required               BOOLEAN NOT NULL DEFAULT FALSE,
                        waitlist_when_all_reserved          BOOLEAN NOT NULL DEFAULT FALSE,
                        organizer_keycloak_id               VARCHAR(36) NOT NULL,
                        created_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                        updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_organizer ON events(organizer_keycloak_id);
CREATE INDEX idx_events_date ON events(event_date);

--rollback DROP TABLE events;