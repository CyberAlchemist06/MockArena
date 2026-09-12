package com.mockarena.question.infrastructure.persistence;

import com.mockarena.question.domain.TaxonomyAssignment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Persistence for data-driven taxonomy labels.  It deliberately has no DSA enum. */
@Repository
public class QuestionVersionTaxonomyRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public QuestionVersionTaxonomyRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void replace(UUID questionVersionId, List<TaxonomyAssignment> assignments) {
        MapSqlParameterSource id = new MapSqlParameterSource("id", questionVersionId);
        jdbc.update("DELETE FROM question.question_version_taxonomy WHERE question_version_id = :id", id);
        for (TaxonomyAssignment assignment : assignments) {
            jdbc.update("""
                    INSERT INTO question.question_version_taxonomy (question_version_id, scheme, code)
                    VALUES (:id, :scheme, :code)
                    """, new MapSqlParameterSource()
                    .addValue("id", questionVersionId)
                    .addValue("scheme", assignment.scheme())
                    .addValue("code", assignment.code()));
        }
    }
}
