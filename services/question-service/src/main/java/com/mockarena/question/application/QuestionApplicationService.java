package com.mockarena.question.application;

import com.mockarena.question.api.QuestionDtos.*;
import com.mockarena.question.domain.*;
import com.mockarena.question.infrastructure.persistence.QuestionVersionTaxonomyRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class QuestionApplicationService {
    private final QuestionRepository questions; private final QuestionVersionRepository versions; private final QuestionVersionTaxonomyRepository taxonomy; private final Clock clock;
    public QuestionApplicationService(QuestionRepository questions, QuestionVersionRepository versions, QuestionVersionTaxonomyRepository taxonomy) { this.questions=questions; this.versions=versions; this.taxonomy=taxonomy; this.clock=Clock.systemUTC(); }

    @Transactional
    public QuestionVersion create(CreateQuestionRequest request, UUID ownerUserId) {
        Instant now=clock.instant(); UUID questionId=UUID.randomUUID();
        questions.save(new Question(questionId, ownerUserId, now));
        QuestionVersion version = versions.save(new QuestionVersion(UUID.randomUUID(), questionId, 1, content(request.content()), now));
        versions.flush();
        taxonomy.replace(version.id(), legacyTaxonomy(version.tags()));
        return version;
    }
    @Transactional(readOnly=true) public Question getQuestion(UUID id) { return questions.findById(id).orElseThrow(() -> new NoSuchElementException("Question not found")); }
    @Transactional(readOnly=true) public List<QuestionVersion> listVersions(UUID questionId) { getQuestion(questionId); return versions.findByQuestionIdOrderByVersionNumberDesc(questionId); }
    @Transactional(readOnly=true) public QuestionVersion getVersion(UUID questionId, int number) { return versions.findByQuestionIdAndVersionNumber(questionId, number).orElseThrow(() -> new NoSuchElementException("Question version not found")); }
    @Transactional
    public QuestionVersion update(UUID questionId, int number, UpdateVersionRequest request) {
        QuestionVersion v=getVersion(questionId, number); assertVersion(v.version(), request.expectedVersion()); v.update(content(request.content()), clock.instant());
        QuestionVersion saved = versions.saveAndFlush(v); taxonomy.replace(saved.id(), legacyTaxonomy(saved.tags())); return saved;
    }
    @Transactional
    public QuestionVersion createRevision(UUID questionId, CreateRevisionRequest request) {
        Question q=getQuestion(questionId); assertVersion(q.version(), request.expectedQuestionVersion());
        QuestionVersion source=versions.findTopByQuestionIdOrderByVersionNumberDesc(questionId).orElseThrow();
        QuestionVersion result=versions.saveAndFlush(new QuestionVersion(UUID.randomUUID(), questionId, source.versionNumber()+1, content(request.content()), clock.instant()));
        taxonomy.replace(result.id(), legacyTaxonomy(result.tags()));
        q.touch(clock.instant()); questions.saveAndFlush(q); return result;
    }
    @Transactional
    public QuestionVersion publish(UUID questionId, int number, PublishVersionRequest request) {
        Question q=getQuestion(questionId); QuestionVersion v=getVersion(questionId, number);
        assertVersion(q.version(), request.expectedQuestionVersion()); assertVersion(v.version(), request.expectedVersion());
        v.publish(clock.instant()); versions.saveAndFlush(v); q.setCurrentVersion(v.id(), QuestionStatus.PUBLISHED, clock.instant()); questions.saveAndFlush(q); return v;
    }
    private static void assertVersion(long actual, long expected) { if(actual != expected) throw new OptimisticLockException("Stale resource version"); }
    private static QuestionVersion.Content content(ContentRequest c) {
        List<McqOption> options = c.options() == null ? null : c.options().stream().map(option -> new McqOption(option.id(), option.text())).toList();
        return new QuestionVersion.Content(c.title(), c.tags(), c.difficulty(), c.questionType(), c.prompt(), c.constraintsText(), c.examples(), c.supportedLanguages(), c.visibleTests(), c.hiddenTests(), c.scoringRules(), c.executionLimits(), options, c.correctOptionId(), c.explanation(), c.codingExecutionSpec());
    }
    /** V1 tags are retained as a compatibility input and project into generic taxonomy. */
    private static List<TaxonomyAssignment> legacyTaxonomy(List<String> tags) {
        List<TaxonomyAssignment> result = new ArrayList<>();
        // Compatibility data for the existing V1 authoring contract; callers of
        // future generic authoring APIs will provide assignments explicitly.
        tags.forEach(tag -> result.add(new TaxonomyAssignment("topic", tag)));
        return List.copyOf(result);
    }
}
