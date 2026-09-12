package com.mockarena.challenge.application;

import com.mockarena.challenge.question.QuestionCatalogEntry;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DeterministicQuestionSelector {
    public List<QuestionCatalogEntry> select(List<QuestionCatalogEntry> candidates, int requestedCount, UUID seed) {
        List<QuestionCatalogEntry> distinct = candidates.stream().collect(java.util.stream.Collectors.toMap(QuestionCatalogEntry::questionId, candidate -> candidate, (left, right) -> left))
                .values().stream().sorted(Comparator.comparing(candidate -> score(seed, candidate.questionVersionId()))).toList();
        if (distinct.size() < requestedCount) throw new InsufficientQuestionsException();
        return distinct.subList(0, requestedCount);
    }
    private String score(UUID seed, UUID questionVersionId) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest((seed + ":" + questionVersionId).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
}
