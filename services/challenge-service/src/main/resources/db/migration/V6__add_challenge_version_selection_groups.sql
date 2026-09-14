-- Draft metadata only. Published manifests remain authoritative and the existing
-- published-version immutability trigger continues to compare all row columns.
ALTER TABLE challenge.challenge_versions
    ADD COLUMN selection_groups JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD CONSTRAINT challenge_versions_selection_groups_array_check
        CHECK (jsonb_typeof(selection_groups) = 'array');
