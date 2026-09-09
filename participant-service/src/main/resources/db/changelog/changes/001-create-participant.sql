--liquibase formatted sql
--changeset xtirela:001-create-participants

CREATE TABLE participants (
                             id                  SERIAL PRIMARY KEY,
                             keycloakId          TEXT NOT NULL,
                             first_name          TEXT NOT NULL,
                             last_name           TEXT NOT NULL,
                             age                 INTEGER      NOT NULL CHECK (age > 0 AND age <= 150),
                             participant_gender  VARCHAR(20)  NOT NULL CHECK (participant_gender IN ('MALE', 'FEMALE', 'NOT_SPECIFIED'))
);

ALTER TABLE participants ADD CONSTRAINT uk_participant_keycloakId UNIQUE (keycloakId);

--rollback DROP TABLE participants;
