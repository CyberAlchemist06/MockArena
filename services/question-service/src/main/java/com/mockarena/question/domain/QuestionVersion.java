package com.mockarena.question.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "question_versions", schema = "question", uniqueConstraints = @UniqueConstraint(name="question_versions_question_version_unique", columnNames={"question_id", "version_number"}))
public class QuestionVersion {
    @Id private UUID id;
    @Column(nullable=false) private UUID questionId;
    @Column(nullable=false) private int versionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private QuestionVersionStatus status;
    @Column(nullable=false) private String title;
    @Column(nullable=false, columnDefinition="text") private String prompt;
    @Column(columnDefinition="text") private String constraintsText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode examples;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode supportedLanguages;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode visibleTests;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode hiddenTests;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode scoringRules;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode executionLimits;
    @Version private long version;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected QuestionVersion() { }
    public QuestionVersion(UUID id, UUID questionId, int number, Content content, Instant now) { this.id=id; this.questionId=questionId; versionNumber=number; status=QuestionVersionStatus.DRAFT; apply(content, now); createdAt=now; }
    public void update(Content content, Instant now) { if (status != QuestionVersionStatus.DRAFT) throw new IllegalStateException("Only draft versions can be changed"); apply(content, now); }
    public void publish(Instant now) { if (status != QuestionVersionStatus.DRAFT) throw new IllegalStateException("Only draft versions can be published"); status=QuestionVersionStatus.PUBLISHED; updatedAt=now; }
    private void apply(Content c, Instant now) { title=c.title(); prompt=c.prompt(); constraintsText=c.constraintsText(); examples=c.examples(); supportedLanguages=c.supportedLanguages(); visibleTests=c.visibleTests(); hiddenTests=c.hiddenTests(); scoringRules=c.scoringRules(); executionLimits=c.executionLimits(); updatedAt=now; }
    public UUID id(){return id;} public UUID questionId(){return questionId;} public int versionNumber(){return versionNumber;} public QuestionVersionStatus status(){return status;} public String title(){return title;} public String prompt(){return prompt;} public String constraintsText(){return constraintsText;} public JsonNode examples(){return examples;} public JsonNode supportedLanguages(){return supportedLanguages;} public JsonNode visibleTests(){return visibleTests;} public JsonNode scoringRules(){return scoringRules;} public JsonNode executionLimits(){return executionLimits;} public long version(){return version;}
    /** Protected material is intentionally available only to domain/application code, never API DTO mapping. */
    public Content copyForRevision() { return new Content(title, prompt, constraintsText, examples, supportedLanguages, visibleTests, hiddenTests, scoringRules, executionLimits); }
    @Override public String toString() { return "QuestionVersion[id="+id+", questionId="+questionId+", versionNumber="+versionNumber+", status="+status+"]"; }
    public record Content(String title, String prompt, String constraintsText, JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests, JsonNode hiddenTests, JsonNode scoringRules, JsonNode executionLimits) { }
}
