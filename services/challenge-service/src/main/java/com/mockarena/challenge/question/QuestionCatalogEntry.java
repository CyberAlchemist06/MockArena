package com.mockarena.challenge.question;
import java.util.UUID;
public record QuestionCatalogEntry(UUID questionId, UUID questionVersionId, String questionTypeCode) {
    public QuestionCatalogEntry(UUID questionId, UUID questionVersionId) {
        this(questionId, questionVersionId, "LEGACY_UNSPECIFIED");
    }
}
