package com.mockarena.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt_response_save_requests", schema = "assessment", uniqueConstraints = {
    @UniqueConstraint(name = "attempt_response_save_requests_key_unique", columnNames = {"attempt_id", "idempotency_key"}),
    @UniqueConstraint(name = "attempt_response_save_requests_mutation_unique", columnNames = {"attempt_id", "client_mutation_id"})
})
public class AttemptResponseSaveRequest {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID attemptId;
    @Column(nullable = false, updatable = false) private String idempotencyKey;
    @Column(nullable = false, updatable = false) private UUID clientMutationId;
    @Column(nullable = false, updatable = false) private String requestFingerprint;
    @Column(nullable = false, updatable = false) private int globalPosition;
    @Column(nullable = false, updatable = false) private long savedResponseVersion;
    @Column(nullable = false, updatable = false) private String savedResponseTypeCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, updatable = false, columnDefinition = "jsonb") private JsonNode savedResponsePayload;
    @Column(nullable = false) private String state;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected AttemptResponseSaveRequest() { }

    public AttemptResponseSaveRequest(UUID id, UUID attemptId, String idempotencyKey, UUID clientMutationId, String requestFingerprint, int globalPosition, long savedResponseVersion, String savedResponseTypeCode, JsonNode savedResponsePayload, Instant now) {
        this.id = id;
        this.attemptId = attemptId;
        this.idempotencyKey = idempotencyKey;
        this.clientMutationId = clientMutationId;
        this.requestFingerprint = requestFingerprint;
        this.globalPosition = globalPosition;
        this.savedResponseVersion = savedResponseVersion;
        this.savedResponseTypeCode = savedResponseTypeCode;
        this.savedResponsePayload = savedResponsePayload.deepCopy();
        this.state = "SAVED";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID attemptId() { return attemptId; }
    public String requestFingerprint() { return requestFingerprint; }
    public int globalPosition() { return globalPosition; }
    public long savedResponseVersion() { return savedResponseVersion; }
    public String savedResponseTypeCode() { return savedResponseTypeCode; }
    public JsonNode savedResponsePayload() { return savedResponsePayload; }
}
