package com.mockarena.question.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.question.api.QuestionCatalogV2Dtos.*;
import com.mockarena.question.domain.TaxonomyAssignment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class QuestionVersionCatalogV2Repository {
    private final NamedParameterJdbcTemplate jdbc; private final ObjectMapper json;
    public QuestionVersionCatalogV2Repository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    public List<Entry> resolve(ResolveRequest criteria) {
        StringBuilder sql = new StringBuilder("""
            SELECT qv.question_id, qv.id question_version_id, qv.version_number, qv.title,
                   COALESCE(history.question_type_code, qv.question_type_code) question_type_code,
                   COALESCE(history.content_locale, qv.content_locale) content_locale,
                   COALESCE(history.difficulty_scheme, qv.difficulty_scheme) difficulty_scheme,
                   COALESCE(history.difficulty_code, qv.difficulty_code) difficulty_code,
                   COALESCE(history.programming_languages, qv.programming_languages) programming_languages,
                   COALESCE((SELECT jsonb_agg(jsonb_build_object('scheme', t.scheme, 'code', t.code))
                               FROM question.question_version_taxonomy t
                              WHERE t.question_version_id = qv.id), '[]'::jsonb) taxonomy
              FROM question.question_versions qv
              JOIN question.questions q ON q.current_version_id = qv.id
              LEFT JOIN question.question_version_historical_generic_metadata history ON history.question_version_id = qv.id
             WHERE q.lifecycle_status = 'PUBLISHED' AND qv.status = 'PUBLISHED'
            """);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", criteria.limit());
        int i=0;
        for (TaxonomyFilter term : optional(criteria.taxonomyAll())) {
            sql.append(" AND EXISTS (SELECT 1 FROM question.question_version_taxonomy tx WHERE tx.question_version_id=qv.id AND tx.scheme=:ts"+i+" AND tx.code=:tc"+i+")");
            p.addValue("ts"+i, term.scheme()); p.addValue("tc"+i, term.code()); i++;
        }
        if (!optional(criteria.questionTypeCodes()).isEmpty()) { sql.append(" AND COALESCE(history.question_type_code, qv.question_type_code) IN (:types)"); p.addValue("types", criteria.questionTypeCodes()); }
        if (!optional(criteria.contentLocales()).isEmpty()) { sql.append(" AND COALESCE(history.content_locale, qv.content_locale) IN (:locales)"); p.addValue("locales", criteria.contentLocales()); }
        if (!optional(criteria.programmingLanguages()).isEmpty()) { sql.append(" AND COALESCE(history.programming_languages, qv.programming_languages) @> CAST(:languages AS jsonb)"); p.addValue("languages", write(criteria.programmingLanguages())); }
        if (!optional(criteria.difficultyProfiles()).isEmpty()) {
            sql.append(" AND ("); int d=0; for (DifficultyProfile profile : criteria.difficultyProfiles()) { if (d++>0) sql.append(" OR "); sql.append("(COALESCE(history.difficulty_scheme, qv.difficulty_scheme)=:ds"+d+" AND COALESCE(history.difficulty_code, qv.difficulty_code)=:dc"+d+")"); p.addValue("ds"+d,profile.scheme()); p.addValue("dc"+d,profile.code()); } sql.append(")");
        }
        sql.append(" ORDER BY qv.question_id, qv.id LIMIT :limit");
        return jdbc.query(sql.toString(), p, this::map);
    }
    private Entry map(ResultSet rs, int row) throws SQLException {
        List<TaxonomyAssignment> taxonomy = new ArrayList<>();
        try { for (JsonNode item : json.readTree(rs.getString("taxonomy"))) taxonomy.add(new TaxonomyAssignment(item.get("scheme").asText(), item.get("code").asText())); }
        catch (Exception e) { throw new IllegalStateException("Invalid taxonomy metadata", e); }
        String scheme=rs.getString("difficulty_scheme"), code=rs.getString("difficulty_code");
        return new Entry(rs.getObject("question_id", UUID.class), rs.getObject("question_version_id", UUID.class), rs.getInt("version_number"), rs.getString("title"), rs.getString("question_type_code"), rs.getString("content_locale"), taxonomy, scheme == null ? null : new DifficultyProfile(scheme, code), strings(rs.getString("programming_languages")));
    }
    private List<String> strings(String value) { try { if(value==null)return List.of(); return json.readValue(value, new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}); } catch(Exception e){throw new IllegalStateException("Invalid programming language metadata",e);} }
    private String write(Object value) { try { return json.writeValueAsString(value); } catch(Exception e){throw new IllegalStateException("Unable to serialize catalog filter",e);} }
    private static <T> List<T> optional(List<T> list) { return list == null ? List.of() : list; }
}
