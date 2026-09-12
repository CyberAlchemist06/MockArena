package com.mockarena.challenge.infrastructure;

import com.mockarena.challenge.question.QuestionCatalogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class ChallengeCompositionRepository {
    private final JdbcTemplate jdbc;
    public ChallengeCompositionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void save(UUID challengeVersionId, List<QuestionCatalogEntry> entries) {
        for (int index = 0; index < entries.size(); index++) {
            QuestionCatalogEntry entry = entries.get(index);
            jdbc.update("insert into challenge.challenge_version_questions (challenge_version_id, position, question_id, question_version_id) values (?, ?, ?, ?)", challengeVersionId, index + 1, entry.questionId(), entry.questionVersionId());
        }
    }
}
