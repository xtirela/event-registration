--liquibase formatted sql
--changeset xtirela:001-create-participant

CREATE TABLE participant (
                             id                  SERIAL PRIMARY KEY,
                             first_name          TEXT NOT NULL,
                             last_name           TEXT NOT NULL,
                             email               TEXT NOT NULL,
                             age                 INTEGER      NOT NULL CHECK (age > 0 AND age <= 150),
                             participant_gender  VARCHAR(20)  NOT NULL CHECK (participant_gender IN ('MALE', 'FEMALE', 'NOT_SPECIFIED')),
                             registered_at       TIMESTAMPTZ  NOT NULL
);

ALTER TABLE participant ADD CONSTRAINT uk_participant_email UNIQUE (email);

--rollback DROP TABLE participant;
