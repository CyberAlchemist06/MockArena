package com.mockarena.question.application;

import com.mockarena.question.domain.Difficulty;
import java.util.List;
import java.util.UUID;

public record QuestionVersionCatalogEntry(
        UUID questionId,
        UUID questionVersionId,
        int versionNumber,
        String title,
        List<String> tags,
        Difficulty difficulty,
        List<String> supportedLanguages) {
}
