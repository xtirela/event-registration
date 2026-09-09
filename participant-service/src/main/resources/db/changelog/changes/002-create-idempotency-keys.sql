--liquibase formatted sql

--changeset xtirela:002-create-idempotency-keys
CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    http_status VARCHAR(50) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

--rollback DROP TABLE idempotency_keys;