package com.mockarena.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(AttemptItemResponseId.class)
@Table(name = "attempt_item_responses", schema = "assessment")
public class AttemptItemResponse {
    @Id @Column(nullable = false, updatable = false) private UUID attemptId;
    @Id @Column(nullable = false, updatable = false) private int globalPosition;
    @Column(nullable = false, updatable = false) private String questionTypeCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode responsePayload;
    @Version private long version;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected AttemptItemResponse() { }

    public AttemptItemResponse(UUID attemptId, int globalPosition, String questionTypeCode, JsonNode responsePayload, Instant now) {
        this.attemptId = attemptId;
        this.globalPosition = globalPosition;
        this.questionTypeCode = questionTypeCode;
        this.responsePayload = responsePayload.deepCopy();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void replace(JsonNode responsePayload, Instant now) {
        this.responsePayload = responsePayload.deepCopy();
        this.updatedAt = now;
    }

    public UUID attemptId() { return attemptId; }
    public int globalPosition() { return globalPosition; }
    public String questionTypeCode() { return questionTypeCode; }
    public JsonNode responsePayload() { return responsePayload; }
    public long version() { return version; }
}
