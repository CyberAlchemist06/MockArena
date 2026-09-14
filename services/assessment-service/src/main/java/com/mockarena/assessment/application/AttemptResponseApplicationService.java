package com.mockarena.assessment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mockarena.assessment.api.AssessmentDtos.SaveAttemptResponseRequest;
import com.mockarena.assessment.api.AssessmentDtos.SavedAttemptResponse;
import com.mockarena.assessment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class AttemptResponseApplicationService {
    private final AttemptRepository attempts;
    private final AttemptItemRepository items;
    private final AttemptItemResponseRepository responses;
    private final AttemptResponseSaveRequestRepository requests;
    private final Clock clock = Clock.systemUTC();

    public AttemptResponseApplicationService(AttemptRepository attempts, AttemptItemRepository items, AttemptItemResponseRepository responses, AttemptResponseSaveRequestRepository requests) {
        this.attempts = attempts;
        this.items = items;
        this.responses = responses;
        this.requests = requests;
    }

    @Transactional
    public SavedAttemptResponse save(UUID attemptId, int globalPosition, UUID candidateUserId, String idempotencyKey, SaveAttemptResponseRequest request) {
        String key = validateKey(idempotencyKey);
        Attempt attempt = lockOwnedInProgressAttempt(attemptId, candidateUserId, clock.instant());
        AttemptItem item = items.findByAttemptIdAndGlobalPosition(attemptId, globalPosition)
            .orElseThrow(() -> new IllegalArgumentException("Attempt item does not exist"));
        Command command = command(item, request);
        String fingerprint = fingerprint(attemptId, globalPosition, request.expectedResponseVersion(), request.clientMutationId(), command);

        AttemptResponseSaveRequest byKey = requests.findByAttemptIdAndIdempotencyKey(attemptId, key).orElse(null);
        if (byKey != null) return replay(byKey, fingerprint, attemptId);
        AttemptResponseSaveRequest byMutation = requests.findByAttemptIdAndClientMutationId(attemptId, request.clientMutationId()).orElse(null);
        if (byMutation != null) return replay(byMutation, fingerprint, attemptId);

        AttemptItemResponse existing = responses.findByAttemptIdAndGlobalPosition(attemptId, globalPosition).orElse(null);
        if (existing == null) {
            if (request.expectedResponseVersion() != 0) throw new ResponseVersionConflictException();
            existing = responses.saveAndFlush(new AttemptItemResponse(attemptId, globalPosition, item.questionTypeCode(), command.payload(), clock.instant()));
        } else {
            if (existing.version() != request.expectedResponseVersion()) throw new ResponseVersionConflictException();
            existing.replace(command.payload(), clock.instant());
            existing = responses.saveAndFlush(existing);
        }
        requests.saveAndFlush(new AttemptResponseSaveRequest(UUID.randomUUID(), attemptId, key, request.clientMutationId(), fingerprint, globalPosition, existing.version(), existing.questionTypeCode(), existing.responsePayload(), clock.instant()));
        return response(existing);
    }

    @Transactional
    public List<SavedAttemptResponse> savedResponses(UUID attemptId, UUID candidateUserId) {
        lockOwnedInProgressAttempt(attemptId, candidateUserId, clock.instant());
        return responses.findByAttemptIdOrderByGlobalPositionAsc(attemptId).stream().map(this::response).toList();
    }

    private Attempt lockOwnedInProgressAttempt(UUID attemptId, UUID candidateUserId, Instant now) {
        Attempt attempt = attempts.findByIdForUpdate(attemptId).orElseThrow(AttemptNotFoundException::new);
        if (!attempt.candidateUserId().equals(candidateUserId)) throw new SecurityException("Attempt is not owned by current user");
        if (attempt.expireIfDue(now)) attempts.saveAndFlush(attempt);
        if (attempt.status() != AttemptStatus.IN_PROGRESS) throw new IllegalStateException("Attempt is not in progress");
        return attempt;
    }

    private static Command command(AttemptItem item, SaveAttemptResponseRequest request) {
        String type = requiredCode(request.responseTypeCode(), "Response type is required");
        if (!item.questionTypeCode().equals(type)) throw new IllegalArgumentException("Response type does not match attempt item");
        if ("MCQ".equals(type)) {
            if (request.programmingLanguage() != null || request.sourceCode() != null) throw new IllegalArgumentException("MCQ response contains unsupported fields");
            String selected = requiredText(request.selectedOptionId(), "MCQ option is required");
            return new Command(type, JsonNodeFactory.instance.objectNode().put("selectedOptionId", selected), selected, null, null);
        }
        if ("CODING".equals(type)) {
            if (request.selectedOptionId() != null) throw new IllegalArgumentException("Coding response contains unsupported fields");
            String language = requiredCode(request.programmingLanguage(), "Programming language is required");
            String source = requiredSource(request.sourceCode());
            return new Command(type, JsonNodeFactory.instance.objectNode().put("programmingLanguage", language).put("sourceCode", source), null, language, source);
        }
        throw new IllegalArgumentException("Unsupported attempt item response type");
    }

    private SavedAttemptResponse replay(AttemptResponseSaveRequest saved, String fingerprint, UUID attemptId) {
        if (!saved.requestFingerprint().equals(fingerprint)) throw new IdempotencyKeyReusedException();
        return response(saved.attemptId(), saved.globalPosition(), saved.savedResponseTypeCode(), saved.savedResponsePayload(), saved.savedResponseVersion());
    }

    private SavedAttemptResponse response(AttemptItemResponse value) {
        return response(value.attemptId(), value.globalPosition(), value.questionTypeCode(), value.responsePayload(), value.version());
    }

    private SavedAttemptResponse response(UUID attemptId, int globalPosition, String questionTypeCode, JsonNode payload, long version) {
        String selected = "MCQ".equals(questionTypeCode) ? payload.path("selectedOptionId").asText() : null;
        String language = "CODING".equals(questionTypeCode) ? payload.path("programmingLanguage").asText() : null;
        String source = "CODING".equals(questionTypeCode) ? payload.path("sourceCode").asText() : null;
        return new SavedAttemptResponse(attemptId, globalPosition, questionTypeCode, selected, language, source, version);
    }

    private static String validateKey(String value) { return requiredText(value, "Idempotency-Key is required", 200); }
    private static String requiredText(String value, String message) { return requiredText(value, message, 500_000); }
    private static String requiredText(String value, String message, int max) { if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException(message); return value.trim(); }
    private static String requiredSource(String value) { if (value == null || value.isBlank() || value.length() > 500_000) throw new IllegalArgumentException("Source code is required"); return value; }
    private static String requiredCode(String value, String message) { return requiredText(value, message, 64).toUpperCase(Locale.ROOT); }
    private static String fingerprint(UUID attemptId, int position, long expectedVersion, UUID mutationId, Command command) {
        try {
            String value = attemptId + "|" + position + "|" + expectedVersion + "|" + mutationId + "|" + command.type() + "|" + command.selectedOptionId() + "|" + command.programmingLanguage() + "|" + command.sourceCode();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("Unable to fingerprint response request"); }
    }

    private static final class Command {
        private final String type; private final JsonNode payload; private final String selectedOptionId; private final String programmingLanguage; private final String sourceCode;
        private Command(String type, JsonNode payload, String selectedOptionId, String programmingLanguage, String sourceCode) { this.type = type; this.payload = payload; this.selectedOptionId = selectedOptionId; this.programmingLanguage = programmingLanguage; this.sourceCode = sourceCode; }
        String type() { return type; } JsonNode payload() { return payload; } String selectedOptionId() { return selectedOptionId; } String programmingLanguage() { return programmingLanguage; } String sourceCode() { return sourceCode; }
    }
}
