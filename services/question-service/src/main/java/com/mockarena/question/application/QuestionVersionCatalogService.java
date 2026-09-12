package com.mockarena.question.application;

import com.mockarena.question.api.QuestionCatalogDtos.ResolveQuestionVersionsRequest;
import com.mockarena.question.domain.Difficulty;
import com.mockarena.question.infrastructure.persistence.QuestionVersionCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@Service
public class QuestionVersionCatalogService {
    private final QuestionVersionCatalogRepository catalog;

    public QuestionVersionCatalogService(QuestionVersionCatalogRepository catalog) { this.catalog = catalog; }

    @Transactional(readOnly = true)
    public List<QuestionVersionCatalogEntry> resolve(ResolveQuestionVersionsRequest request) {
        return catalog.resolve(new Criteria(normalizeTags(request.tagsAll()), request.difficulties(), normalizeLanguage(request.supportedLanguage()), request.limit()));
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        TreeSet<String> normalized = new TreeSet<>();
        for (String tag : tags) normalized.add(tag.trim().toLowerCase(Locale.ROOT));
        return List.copyOf(normalized);
    }

    private static String normalizeLanguage(String language) {
        return language == null ? null : language.trim().toUpperCase(Locale.ROOT);
    }

    public record Criteria(List<String> tagsAll, List<Difficulty> difficulties, String supportedLanguage, int limit) { }
}
