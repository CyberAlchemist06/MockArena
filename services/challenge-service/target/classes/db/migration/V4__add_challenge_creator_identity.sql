ALTER TABLE challenge.challenges
    ADD COLUMN created_by_user_id UUID;

CREATE INDEX challenges_created_by_user_id_idx
    ON challenge.challenges (created_by_user_id);
