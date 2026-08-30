--liquibase formatted sql
--changeset xtirela:008-create-users

CREATE TABLE users (
    id       SERIAL PRIMARY KEY,
    username TEXT        NOT NULL,
    email    TEXT        NOT NULL,
    password TEXT        NOT NULL,
    role     VARCHAR(20) NOT NULL CHECK (role IN ('PARTICIPANT', 'ADMIN', 'ORGANISER'))
);

ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

--rollback DROP TABLE users;
