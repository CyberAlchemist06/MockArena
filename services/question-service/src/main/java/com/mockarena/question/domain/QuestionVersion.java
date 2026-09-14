package com.mockarena.question.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;

@Entity @Table(name = "question_versions", schema = "question", uniqueConstraints = @UniqueConstraint(name="question_versions_question_version_unique", columnNames={"question_id", "version_number"}))
public class QuestionVersion {
    @Id private UUID id;
    @Column(nullable=false) private UUID questionId;
    @Column(nullable=false) private int versionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private QuestionVersionStatus status;
    @Column(nullable=false) private String title;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private List<String> tags;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Difficulty difficulty;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private QuestionType questionType;
    @Column(nullable=false) private String questionTypeCode;
    @Column(nullable=false) private String contentLocale;
    private String difficultyScheme;
    private String difficultyCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode programmingLanguages;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private JsonNode defaultScoringPolicy;
    @Column(nullable=false, columnDefinition="text") private String prompt;
    @Column(columnDefinition="text") private String constraintsText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode examples;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode supportedLanguages;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode visibleTests;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode hiddenTests;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode scoringRules;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode executionLimits;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private JsonNode codingExecutionSpec;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") private List<McqOption> options;
    private String correctOptionId;
    @Column(columnDefinition="text") private String explanation;
    @Version private long version;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected QuestionVersion() { }
    public QuestionVersion(UUID id, UUID questionId, int number, Content content, Instant now) { this.id=id; this.questionId=questionId; versionNumber=number; status=QuestionVersionStatus.DRAFT; apply(content, now); createdAt=now; }
    public void update(Content content, Instant now) { if (status != QuestionVersionStatus.DRAFT) throw new IllegalStateException("Only draft versions can be changed"); if (questionType != content.questionType()) throw new IllegalStateException("Question type changes require a new version"); apply(content, now); }
    public void publish(Instant now) { if (status != QuestionVersionStatus.DRAFT) throw new IllegalStateException("Only draft versions can be published"); status=QuestionVersionStatus.PUBLISHED; updatedAt=now; }
    private void apply(Content c, Instant now) {
        if (c.questionType() == null) throw new IllegalArgumentException("Question type is required");
        if (c.title() == null || c.title().isBlank() || c.prompt() == null || c.prompt().isBlank() || c.difficulty() == null) throw new IllegalArgumentException("Question content is incomplete");
        title=c.title(); tags=normalizeTags(c.tags()); difficulty=c.difficulty(); questionType=c.questionType(); prompt=c.prompt();
        // The enum remains the V1 handler discriminator.  These values are the
        // generic, data-facing representation used by V2 catalog clients.
        questionTypeCode=questionType.name(); contentLocale="en";
        difficultyScheme="mockarena-v1"; difficultyCode=difficulty.name();
        if (questionType == QuestionType.CODING) {
            requireCoding(c);
            CodingExecutionSpec executable = c.codingExecutionSpec() == null ? null : CodingExecutionSpec.parse(c.codingExecutionSpec());
            if (executable != null) executable.requireMatchingSupportedLanguages(c.supportedLanguages());
            constraintsText=c.constraintsText(); examples=c.examples(); supportedLanguages=c.supportedLanguages(); visibleTests=c.visibleTests(); codingExecutionSpec=executable == null ? null : executable.canonical();
            hiddenTests=executable == null ? c.hiddenTests() : executable.hiddenTests(); scoringRules=executable == null ? c.scoringRules() : executable.scoringPolicy(); executionLimits=executable == null ? c.executionLimits() : executable.executionLimits();
            programmingLanguages=executable == null ? c.supportedLanguages() : executable.allowedProgrammingLanguages();
            defaultScoringPolicy=JsonNodeFactory.instance.objectNode().put("policyCode", "TEST_CASES").set("parameters", scoringRules.deepCopy());
            options=List.of(); correctOptionId=null; explanation=null;
        } else {
            validateMcq(c);
            constraintsText=null; examples=null; supportedLanguages=null; programmingLanguages=JsonNodeFactory.instance.arrayNode(); visibleTests=null; hiddenTests=null; scoringRules=null; executionLimits=null; codingExecutionSpec=null;
            defaultScoringPolicy=JsonNodeFactory.instance.objectNode().put("policyCode", "FIXED_RESPONSE").set("parameters", JsonNodeFactory.instance.objectNode().put("correctPoints", 1).put("incorrectPoints", 0).put("unansweredPoints", 0));
            options=List.copyOf(c.options()); correctOptionId=c.correctOptionId(); explanation=c.explanation();
        }
        updatedAt=now;
    }
    private static void requireCoding(Content c) {
        if (c.constraintsText() == null || c.constraintsText().isBlank() || c.examples() == null || c.supportedLanguages() == null || c.visibleTests() == null || (c.codingExecutionSpec() == null && (c.hiddenTests() == null || c.scoringRules() == null || c.executionLimits() == null))) throw new IllegalArgumentException("Coding questions require coding content");
        if ((c.options() != null && !c.options().isEmpty()) || c.correctOptionId() != null || c.explanation() != null) throw new IllegalArgumentException("Coding questions must not include MCQ content");
    }
    private static void validateMcq(Content c) {
        if (c.constraintsText() != null || c.examples() != null || c.supportedLanguages() != null || c.visibleTests() != null || c.hiddenTests() != null || c.scoringRules() != null || c.executionLimits() != null) throw new IllegalArgumentException("MCQ questions must not include coding content");
        if (c.options() == null || c.options().size() < 2 || c.correctOptionId() == null || c.correctOptionId().isBlank()) throw new IllegalArgumentException("MCQ questions require at least two options and one correct answer");
        TreeSet<String> ids = new TreeSet<>();
        for (McqOption option : c.options()) {
            if (option == null || option.id() == null || option.id().isBlank() || option.text() == null || option.text().isBlank() || !ids.add(option.id())) throw new IllegalArgumentException("MCQ option IDs must be unique and non-blank");
        }
        if (!ids.contains(c.correctOptionId())) throw new IllegalArgumentException("Correct MCQ answer must reference an option");
    }
    private static List<String> normalizeTags(List<String> input) {
        if (input == null) throw new IllegalArgumentException("Tags are required");
        TreeSet<String> normalized = new TreeSet<>();
        for (String tag : input) {
            if (tag == null || tag.isBlank()) throw new IllegalArgumentException("Tags must not be blank");
            normalized.add(tag.trim().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(normalized);
    }
    public UUID id(){return id;} public UUID questionId(){return questionId;} public int versionNumber(){return versionNumber;} public QuestionVersionStatus status(){return status;} public String title(){return title;} public List<String> tags(){return tags;} public Difficulty difficulty(){return difficulty;} public QuestionType questionType(){return questionType;} public String questionTypeCode(){return questionTypeCode;} public McqEvaluationData mcqEvaluationData(){if(questionType!=QuestionType.MCQ)throw new IllegalStateException("Question version is not MCQ");return new McqEvaluationData(id,questionTypeCode,correctOptionId,defaultScoringPolicy.deepCopy());} public CodingEvaluationData codingEvaluationData(){if(questionType!=QuestionType.CODING||codingExecutionSpec==null)throw new IllegalStateException("Question version is not executable coding");CodingExecutionSpec spec=CodingExecutionSpec.parse(codingExecutionSpec);return new CodingEvaluationData(id,questionTypeCode,spec.runtimeProfileId(),spec.allowedProgrammingLanguages(),spec.executionLimits(),spec.scoringPolicy(),spec.testSpecification());} public String contentLocale(){return contentLocale;} public String difficultyScheme(){return difficultyScheme;} public String difficultyCode(){return difficultyCode;} public JsonNode programmingLanguages(){return programmingLanguages;} public JsonNode defaultScoringPolicy(){return defaultScoringPolicy;} public String prompt(){return prompt;} public String constraintsText(){return constraintsText;} public JsonNode examples(){return examples;} public JsonNode supportedLanguages(){return supportedLanguages;} public JsonNode visibleTests(){return visibleTests;} public JsonNode scoringRules(){return scoringRules;} public JsonNode executionLimits(){return executionLimits;} public List<McqOption> options(){return options;} public long version(){return version;}
    /** Protected material is intentionally available only to domain/application code, never API DTO mapping. */
    public Content copyForRevision() { return new Content(title, tags, difficulty, questionType, prompt, constraintsText, examples, supportedLanguages, visibleTests, hiddenTests, scoringRules, executionLimits, options, correctOptionId, explanation, codingExecutionSpec); }
    @Override public String toString() { return "QuestionVersion[id="+id+", questionId="+questionId+", versionNumber="+versionNumber+", status="+status+"]"; }
    public record Content(String title, List<String> tags, Difficulty difficulty, QuestionType questionType, String prompt, String constraintsText, JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests, JsonNode hiddenTests, JsonNode scoringRules, JsonNode executionLimits, List<McqOption> options, String correctOptionId, String explanation, JsonNode codingExecutionSpec) {
        public Content(String title, List<String> tags, Difficulty difficulty, QuestionType questionType, String prompt, String constraintsText, JsonNode examples, JsonNode supportedLanguages, JsonNode visibleTests, JsonNode hiddenTests, JsonNode scoringRules, JsonNode executionLimits, List<McqOption> options, String correctOptionId, String explanation) { this(title,tags,difficulty,questionType,prompt,constraintsText,examples,supportedLanguages,visibleTests,hiddenTests,scoringRules,executionLimits,options,correctOptionId,explanation,null); }
        @Override public String toString() { return "QuestionVersion.Content[title=" + title + ", questionType=" + questionType + "]"; }
    }
}
