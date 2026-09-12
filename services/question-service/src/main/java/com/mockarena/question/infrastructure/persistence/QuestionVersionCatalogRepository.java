package com.mockarena.question.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.question.application.QuestionVersionCatalogEntry;
import com.mockarena.question.application.QuestionVersionCatalogService.Criteria;
import com.mockarena.question.domain.Difficulty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class QuestionVersionCatalogRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public QuestionVersionCatalogRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<QuestionVersionCatalogEntry> resolve(Criteria criteria) {
        StringBuilder sql = new StringBuilder("""
            SELECT qv.question_id, qv.id AS question_version_id, qv.version_number, qv.title,
                   qv.tags, qv.difficulty, qv.supported_languages
              FROM question.question_versions qv
              JOIN question.questions q ON q.current_version_id = qv.id
             WHERE q.lifecycle_status = 'PUBLISHED'
               AND qv.status = 'PUBLISHED'
            """);
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("limit", criteria.limit());
        if (!criteria.tagsAll().isEmpty()) {
            sql.append(" AND qv.tags @> CAST(:tags AS jsonb)");
            parameters.addValue("tags", json(criteria.tagsAll()));
        }
        if (criteria.difficulties() != null && !criteria.difficulties().isEmpty()) {
            sql.append(" AND qv.difficulty IN (:difficulties)");
            parameters.addValue("difficulties", criteria.difficulties().stream().map(Enum::name).toList());
        }
        if (criteria.supportedLanguage() != null) {
            sql.append(" AND qv.supported_languages @> CAST(:supportedLanguage AS jsonb)");
            parameters.addValue("supportedLanguage", json(List.of(criteria.supportedLanguage())));
        }
        sql.append(" ORDER BY qv.question_id, qv.id LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, this::map);
    }

    private QuestionVersionCatalogEntry map(ResultSet resultSet, int rowNum) throws SQLException {
        return new QuestionVersionCatalogEntry(
                resultSet.getObject("question_id", java.util.UUID.class),
                resultSet.getObject("question_version_id", java.util.UUID.class),
                resultSet.getInt("version_number"),
                resultSet.getString("title"),
                readStrings(resultSet.getString("tags")),
                Difficulty.valueOf(resultSet.getString("difficulty")),
                readStrings(resultSet.getString("supported_languages")));
    }

    private List<String> readStrings(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid catalog metadata in persistence", e);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize catalog filter", e);
        }
    }
}
