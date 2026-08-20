--liquibase formatted sql
--changeset xtirela:003-create-event-registration
CREATE TABLE event_registration (
                                    id                    SERIAL PRIMARY KEY,
                                    participant_id        INTEGER     NOT NULL,
                                    event_id              INTEGER     NOT NULL,
                                    event_reg_request_status VARCHAR(20) NOT NULL CHECK (event_reg_request_status IN ('ACCEPTED', 'PENDING', 'DENIED', 'CANCELLED', 'NOT_FOUND', 'DEPRECATED', 'WAITING')),
                                    description           TEXT        NOT NULL DEFAULT 'none',
                                    CONSTRAINT fk_registration_participant FOREIGN KEY (participant_id) REFERENCES participant (id),
                                    CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES event (id)
);

CREATE INDEX idx_registration_event ON event_registration (event_id);
CREATE INDEX idx_registration_participant ON event_registration (participant_id);

--rollback DROP TABLE event_registration;3