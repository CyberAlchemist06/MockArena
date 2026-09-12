package com.mockarena.challenge;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ChallengeSelectionMigrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void backfillsOnlyLegacyDraftSelectionAndLeavesPublishedManifestUntouched() throws Exception {
        Flyway v1 = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("challenge").locations("filesystem:src/main/resources/db/migration").target("1").load();
        v1.migrate();
        UUID challenge = UUID.randomUUID(), draft = UUID.randomUUID(), published = UUID.randomUUID();
        try (Connection connection = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.prepareStatement("""
                INSERT INTO challenge.challenges (id, visibility, lifecycle_status, version, created_at, updated_at) VALUES (?, 'PRIVATE', 'DRAFT', 0, ?, ?);
                INSERT INTO challenge.challenge_versions (id, challenge_id, version_number, status, title, tags_all, difficulties, supported_language, requested_question_count, selection_seed, version, created_at, updated_at)
                VALUES (?, ?, 1, 'DRAFT', 'Draft', '[\"trees\"]', '[\"MEDIUM\"]', 'JAVA', 1, ?, 0, ?, ?),
                       (?, ?, 2, 'PUBLISHED', 'Published', '[\"arrays\"]', '[\"EASY\"]', 'JAVA', 1, ?, 0, ?, ?);
                """)) {
            Instant now = Instant.now();
            java.sql.Timestamp timestamp = java.sql.Timestamp.from(now);
            statement.setObject(1, challenge); statement.setTimestamp(2, timestamp); statement.setTimestamp(3, timestamp);
            statement.setObject(4, draft); statement.setObject(5, challenge); statement.setObject(6, UUID.randomUUID()); statement.setTimestamp(7, timestamp); statement.setTimestamp(8, timestamp);
            statement.setObject(9, published); statement.setObject(10, challenge); statement.setObject(11, UUID.randomUUID()); statement.setTimestamp(12, timestamp); statement.setTimestamp(13, timestamp);
            statement.executeUpdate();
        }
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("challenge").locations("filesystem:src/main/resources/db/migration").load().migrate();
        try (Connection connection = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.prepareStatement("SELECT status, taxonomy_all::text, difficulty_profiles::text, programming_languages::text FROM challenge.challenge_versions ORDER BY version_number");
             var result = statement.executeQuery()) {
            result.next(); assertThat(result.getString(1)).isEqualTo("DRAFT"); assertThat(result.getString(2)).contains("trees"); assertThat(result.getString(3)).contains("mockarena-v1").contains("MEDIUM"); assertThat(result.getString(4)).isEqualTo("[\"JAVA\"]");
            result.next(); assertThat(result.getString(1)).isEqualTo("PUBLISHED"); assertThat(result.getString(2)).isEqualTo("[]"); assertThat(result.getString(3)).isEqualTo("[]"); assertThat(result.getString(4)).isEqualTo("[]");
        }
    }
}
