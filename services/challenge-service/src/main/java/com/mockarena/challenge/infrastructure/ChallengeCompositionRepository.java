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
            jdbc.update("insert into challenge.challenge_version_questions (challenge_version_id, position, question_id, question_version_id, question_type_code) values (?, ?, ?, ?, ?)", challengeVersionId, index + 1, entry.questionId(), entry.questionVersionId(), entry.questionTypeCode());
        }
    }
    public void replace(UUID challengeVersionId, List<QuestionCatalogEntry> entries) {
        jdbc.update("delete from challenge.challenge_version_questions where challenge_version_id = ?", challengeVersionId);
        save(challengeVersionId, entries);
    }
    public List<QuestionCatalogEntry> findByChallengeVersionId(UUID challengeVersionId) {
        return jdbc.query("select question_id, question_version_id, question_type_code from challenge.challenge_version_questions where challenge_version_id = ? order by position", (rs, row) -> new QuestionCatalogEntry(rs.getObject("question_id", UUID.class), rs.getObject("question_version_id", UUID.class), rs.getString("question_type_code")), challengeVersionId);
    }
    public int countByChallengeVersionId(UUID challengeVersionId) {
        Integer result = jdbc.queryForObject("select count(*) from challenge.challenge_version_questions where challenge_version_id = ?", Integer.class, challengeVersionId);
        return result == null ? 0 : result;
    }
}
