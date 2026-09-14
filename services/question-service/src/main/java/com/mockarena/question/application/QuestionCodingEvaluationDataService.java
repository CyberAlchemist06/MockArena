package com.mockarena.question.application;

import com.mockarena.question.domain.CodingEvaluationData;
import com.mockarena.question.domain.QuestionVersion;
import com.mockarena.question.domain.QuestionVersionRepository;
import com.mockarena.question.domain.QuestionVersionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class QuestionCodingEvaluationDataService {
    private final QuestionVersionRepository versions;

    public QuestionCodingEvaluationDataService(QuestionVersionRepository versions) { this.versions = versions; }

    @Transactional(readOnly = true)
    public List<CodingEvaluationData> resolve(List<UUID> ids) {
        Map<UUID, QuestionVersion> found = new HashMap<>();
        versions.findAllById(ids).forEach(version -> found.put(version.id(), version));
        return ids.stream().map(id -> {
            QuestionVersion version = Optional.ofNullable(found.get(id)).orElseThrow(() -> new NoSuchElementException("Question version not found"));
            if (version.status() != QuestionVersionStatus.PUBLISHED && version.status() != QuestionVersionStatus.RETIRED) {
                throw new NoSuchElementException("Question version not evaluable");
            }
            try {
                return version.codingEvaluationData();
            } catch (IllegalStateException exception) {
                // A legacy coding version is deliberately non-executable. Keep the
                // public error shape indistinguishable from an unavailable version.
                throw new NoSuchElementException("Question version not executable");
            }
        }).toList();
    }
}
