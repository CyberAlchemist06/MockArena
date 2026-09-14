package com.mockarena.assessment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.assessment.api.PublicAssessmentDtos;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class PublicAssessmentCatalogueService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    public PublicAssessmentCatalogueService(NamedParameterJdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public PublicAssessmentDtos.CataloguePage list(String q, String assessmentTypeCode, String availability, Integer pageSize, String cursor) {
        Query query = Query.of(q, assessmentTypeCode, availability, pageSize, cursor, json);
        String where = eligibility() + query.filters();
        MapSqlParameterSource params = query.parameters();
        String sql = "select p.* from assessment.public_assessment_catalogue p join assessment.assessments a on a.id=p.assessment_id join assessment.assessment_versions av on av.id=p.assessment_version_id where " + where + " order by p.published_at desc, p.assessment_id asc limit :limit";
        List<Row> rows;
        try { rows = jdbc.query(sql, params, (rs, ignored) -> Row.from(rs, json)); }
        catch (org.springframework.dao.DataAccessException exception) { throw new PublicCatalogueUnavailableException(exception); }
        boolean hasMore = rows.size() > query.pageSize(); if (hasMore) rows.remove(rows.size() - 1);
        String next = hasMore && !rows.isEmpty() ? query.cursorFor(rows.getLast(), json) : null;
        return new PublicAssessmentDtos.CataloguePage(rows.stream().map(Row::summary).toList(), next);
    }

    public PublicAssessmentDtos.AssessmentDetail detail(UUID assessmentId) {
        String sql = "select p.* from assessment.public_assessment_catalogue p join assessment.assessments a on a.id=p.assessment_id join assessment.assessment_versions av on av.id=p.assessment_version_id where " + eligibility() + " and p.assessment_id=:assessmentId";
        List<Row> rows;
        try { rows = jdbc.query(sql, new MapSqlParameterSource("assessmentId", assessmentId), (rs, ignored) -> Row.from(rs, json)); }
        catch (org.springframework.dao.DataAccessException exception) { throw new PublicCatalogueUnavailableException(exception); }
        if (rows.isEmpty()) throw new AssessmentNotFoundException();
        return rows.getFirst().detail();
    }

    private static String eligibility() { return "a.lifecycle_status='PUBLISHED' and a.visibility='PUBLIC' and a.current_published_version_id=p.assessment_version_id and av.status='PUBLISHED'"; }

    private record Row(UUID assessmentId, int versionNumber, String title, String description, String instructionsSummary, String assessmentTypeCode, String timingPolicyCode, Integer attemptDurationSeconds, Instant availableFrom, Instant availableUntil, int maxAttempts, String resultReleasePolicyCode, Instant resultReleaseAt, int questionCount, Map<String, Integer> questionTypeCounts, Instant publishedAt) {
        static Row from(java.sql.ResultSet rs, ObjectMapper json) throws java.sql.SQLException {
            JsonNode counts; try { counts = json.readTree(rs.getString("question_type_counts")); } catch (java.io.IOException exception) { throw new java.sql.SQLException("Invalid catalogue projection", exception); } Map<String, Integer> values = new TreeMap<>(); counts.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asInt()));
            return new Row(rs.getObject("assessment_id", UUID.class), rs.getInt("version_number"), rs.getString("title"), rs.getString("description"), rs.getString("instructions_summary"), rs.getString("assessment_type_code"), rs.getString("timing_policy_code"), rs.getObject("attempt_duration_seconds", Integer.class), instant(rs, "available_from"), instant(rs, "available_until"), rs.getInt("max_attempts"), rs.getString("result_release_policy_code"), instant(rs, "result_release_at"), rs.getInt("question_count"), Map.copyOf(values), instant(rs, "published_at"));
        }
        static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException { java.sql.Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toInstant(); }
        PublicAssessmentDtos.AssessmentSummary summary() { return new PublicAssessmentDtos.AssessmentSummary(assessmentId, versionNumber, title, description, assessmentTypeCode, "PUBLIC", new PublicAssessmentDtos.TimingSummary(timingPolicyCode, attemptDurationSeconds), new PublicAssessmentDtos.AvailabilitySummary(availableFrom, availableUntil), questionCount, questionTypeCounts); }
        PublicAssessmentDtos.AssessmentDetail detail() { return new PublicAssessmentDtos.AssessmentDetail(assessmentId, versionNumber, title, description, instructionsSummary, assessmentTypeCode, "PUBLIC", new PublicAssessmentDtos.TimingSummary(timingPolicyCode, attemptDurationSeconds), new PublicAssessmentDtos.AvailabilitySummary(availableFrom, availableUntil), new PublicAssessmentDtos.AttemptPolicySummary("MAX_ATTEMPTS", maxAttempts), new PublicAssessmentDtos.ResultReleaseSummary(resultReleasePolicyCode, resultReleaseAt), questionCount, questionTypeCounts); }
    }

    private record Query(String q, String type, Availability availability, int pageSize, Instant asOf, Cursor cursor, String fingerprint) {
        static Query of(String rawQ, String rawType, String rawAvailability, Integer rawPageSize, String encodedCursor, ObjectMapper json) {
            String q = rawQ == null ? null : rawQ.trim(); if (q != null && (q.isEmpty() || q.length() > 100)) throw new IllegalArgumentException("Invalid query");
            String type = rawType == null ? null : rawType.trim().toUpperCase(Locale.ROOT); if (type != null && (type.isEmpty() || type.length() > 64)) throw new IllegalArgumentException("Invalid query");
            Availability availability = rawAvailability == null || rawAvailability.isBlank() ? Availability.DEFAULT : Availability.valueOf(rawAvailability.trim().toUpperCase(Locale.ROOT));
            int size = rawPageSize == null ? 20 : rawPageSize; if (size < 1 || size > 50) throw new IllegalArgumentException("Invalid query");
            String fingerprint = fingerprint(q, type, availability); Cursor cursor = encodedCursor == null || encodedCursor.isBlank() ? null : decode(encodedCursor, json);
            if (cursor != null && (!fingerprint.equals(cursor.filterFingerprint()) || cursor.version() != 1)) throw new IllegalArgumentException("Invalid cursor");
            return new Query(q, type, availability, size, cursor == null ? Instant.now() : cursor.asOf(), cursor, fingerprint);
        }
        String filters() { StringBuilder filters = new StringBuilder(); if (q != null) filters.append(" and (lower(p.title) like :search or lower(coalesce(p.description,'')) like :search)"); if (type != null) filters.append(" and p.assessment_type_code=:type"); switch (availability) { case DEFAULT -> filters.append(" and (p.available_until is null or p.available_until>:asOf)"); case OPEN -> filters.append(" and (p.available_from is null or p.available_from<=:asOf) and (p.available_until is null or p.available_until>:asOf)"); case UPCOMING -> filters.append(" and p.available_from>:asOf"); case ALL -> { } } if (cursor != null) filters.append(" and (p.published_at<:publishedAt or (p.published_at=:publishedAt and p.assessment_id>:assessmentId))"); return filters.toString(); }
        MapSqlParameterSource parameters() { MapSqlParameterSource parameters = new MapSqlParameterSource("asOf", java.sql.Timestamp.from(asOf)).addValue("limit", pageSize + 1); if (q != null) parameters.addValue("search", "%" + q.toLowerCase(Locale.ROOT) + "%"); if (type != null) parameters.addValue("type", type); if (cursor != null) parameters.addValue("publishedAt", java.sql.Timestamp.from(cursor.publishedAt())).addValue("assessmentId", cursor.assessmentId()); return parameters; }
        String cursorFor(Row row, ObjectMapper json) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(new Cursor(1, asOf, row.publishedAt(), row.assessmentId(), fingerprint))); } catch (Exception exception) { throw new IllegalStateException("Could not encode cursor", exception); } }
        static Cursor decode(String encoded, ObjectMapper json) { try { return json.readValue(Base64.getUrlDecoder().decode(encoded), Cursor.class); } catch (Exception exception) { throw new IllegalArgumentException("Invalid cursor"); } }
        static String fingerprint(String q, String type, Availability availability) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest((String.valueOf(q) + "|" + type + "|" + availability).getBytes(StandardCharsets.UTF_8)); return Base64.getUrlEncoder().withoutPadding().encodeToString(hash); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    }
    private record Cursor(int version, Instant asOf, Instant publishedAt, UUID assessmentId, String filterFingerprint) { }
    private enum Availability { DEFAULT, OPEN, UPCOMING, ALL }
}
