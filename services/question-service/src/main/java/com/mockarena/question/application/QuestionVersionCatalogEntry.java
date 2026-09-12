package com.mockarena.question.application;

import com.mockarena.question.domain.Difficulty;
import com.mockarena.question.domain.QuestionType;
import java.util.List;
import java.util.UUID;

public record QuestionVersionCatalogEntry(
        UUID questionId,
        UUID questionVersionId,
        int versionNumber,
        String title,
        List<String> tags,
        Difficulty difficulty,
        QuestionType questionType,
        List<String> supportedLanguages) {
}
