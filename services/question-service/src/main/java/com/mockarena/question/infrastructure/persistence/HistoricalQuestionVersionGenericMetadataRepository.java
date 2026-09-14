package com.mockarena.question.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Read-only generic metadata snapshots for versions that predate the V4 model. */
@Repository
public class HistoricalQuestionVersionGenericMetadataRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    public HistoricalQuestionVersionGenericMetadataRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
    public Map<UUID, Metadata> findByQuestionVersionIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, Metadata> result = new HashMap<>();
        jdbc.query("""
                SELECT question_version_id, question_type_code, programming_languages
                  FROM question.question_version_historical_generic_metadata
                 WHERE question_version_id IN (:ids)
                """, new MapSqlParameterSource("ids", ids), (RowCallbackHandler) rs -> result.put(
                rs.getObject("question_version_id", UUID.class),
                new Metadata(rs.getString("question_type_code"), read(rs.getString("programming_languages")))));
        return result;
    }
    private JsonNode read(String value) { try { return json.readTree(value); } catch (Exception e) { throw new IllegalStateException("Invalid historical question version generic metadata", e); } }
    public record Metadata(String questionTypeCode, JsonNode programmingLanguages) { }
}
