--liquibase formatted sql
--changeset xtirela:011-alter-participants

-- связь участник -> пользователь (Participant.user -> user_id, @OneToOne)
ALTER TABLE participants ADD COLUMN user_id INTEGER;
ALTER TABLE participants ADD CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE participants ADD CONSTRAINT uk_participant_user UNIQUE (user_id);

-- лишние колонки, отсутствующие в сущности Participant
ALTER TABLE participants DROP COLUMN IF EXISTS email;
ALTER TABLE participants DROP COLUMN IF EXISTS registered_at;

--rollback ALTER TABLE participants DROP CONSTRAINT fk_participant_user;
--rollback ALTER TABLE participants DROP CONSTRAINT uk_participant_user;
--rollback ALTER TABLE participants DROP COLUMN user_id;
