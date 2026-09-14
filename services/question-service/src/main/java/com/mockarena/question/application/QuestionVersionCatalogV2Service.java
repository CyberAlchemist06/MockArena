package com.mockarena.question.application;

import com.mockarena.question.api.QuestionCatalogV2Dtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.question.infrastructure.persistence.QuestionVersionCatalogV2Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class QuestionVersionCatalogV2Service {
    private final QuestionVersionCatalogV2Repository repository;
    private final ObjectMapper json;
    public QuestionVersionCatalogV2Service(QuestionVersionCatalogV2Repository repository, ObjectMapper json) { this.repository = repository; this.json = json; }
    @Transactional(readOnly = true)
    public ResolveResponse resolve(ResolveRequest request) {
        Cursor cursor = decode(request.cursor());
        List<Entry> candidates = repository.resolve(request, cursor == null ? null : new QuestionVersionCatalogV2Repository.CursorKey(cursor.questionId(), cursor.questionVersionId()), request.limit() + 1);
        boolean hasMore = candidates.size() > request.limit();
        List<Entry> entries = hasMore ? candidates.subList(0, request.limit()) : candidates;
        return new ResolveResponse(entries, hasMore ? encode(entries.get(entries.size() - 1)) : null);
    }
    private Cursor decode(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            Cursor cursor = json.readValue(Base64.getUrlDecoder().decode(value), Cursor.class);
            if (cursor.version() != 1 || cursor.questionId() == null || cursor.questionVersionId() == null) throw new IllegalArgumentException("Invalid catalog cursor");
            return cursor;
        } catch (IllegalArgumentException exception) { throw exception;
        } catch (Exception exception) { throw new IllegalArgumentException("Invalid catalog cursor"); }
    }
    private String encode(Entry entry) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(new Cursor(1, entry.questionId(), entry.questionVersionId()))); }
        catch (Exception exception) { throw new IllegalStateException("Unable to encode catalog cursor", exception); }
    }
    private record Cursor(int version, UUID questionId, UUID questionVersionId) { }
}
