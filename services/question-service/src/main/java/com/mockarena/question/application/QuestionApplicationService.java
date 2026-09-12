package com.mockarena.question.application;

import com.mockarena.question.api.QuestionDtos.*;
import com.mockarena.question.domain.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class QuestionApplicationService {
    private final QuestionRepository questions; private final QuestionVersionRepository versions; private final Clock clock;
    public QuestionApplicationService(QuestionRepository questions, QuestionVersionRepository versions) { this.questions=questions; this.versions=versions; this.clock=Clock.systemUTC(); }

    @Transactional
    public QuestionVersion create(CreateQuestionRequest request) {
        Instant now=clock.instant(); UUID questionId=UUID.randomUUID();
        questions.save(new Question(questionId, request.ownerUserId(), now));
        return versions.save(new QuestionVersion(UUID.randomUUID(), questionId, 1, content(request.content()), now));
    }
    @Transactional(readOnly=true) public Question getQuestion(UUID id) { return questions.findById(id).orElseThrow(() -> new NoSuchElementException("Question not found")); }
    @Transactional(readOnly=true) public List<QuestionVersion> listVersions(UUID questionId) { getQuestion(questionId); return versions.findByQuestionIdOrderByVersionNumberDesc(questionId); }
    @Transactional(readOnly=true) public QuestionVersion getVersion(UUID questionId, int number) { return versions.findByQuestionIdAndVersionNumber(questionId, number).orElseThrow(() -> new NoSuchElementException("Question version not found")); }
    @Transactional
    public QuestionVersion update(UUID questionId, int number, UpdateVersionRequest request) {
        QuestionVersion v=getVersion(questionId, number); assertVersion(v.version(), request.expectedVersion()); v.update(content(request.content()), clock.instant()); return versions.saveAndFlush(v);
    }
    @Transactional
    public QuestionVersion createRevision(UUID questionId, long expectedQuestionVersion) {
        Question q=getQuestion(questionId); assertVersion(q.version(), expectedQuestionVersion);
        QuestionVersion source=versions.findTopByQuestionIdOrderByVersionNumberDesc(questionId).orElseThrow();
        QuestionVersion.Content c=source.copyForRevision();
        QuestionVersion result=versions.save(new QuestionVersion(UUID.randomUUID(), questionId, source.versionNumber()+1, c, clock.instant()));
        q.touch(clock.instant()); questions.saveAndFlush(q); return result;
    }
    @Transactional
    public QuestionVersion publish(UUID questionId, int number, PublishVersionRequest request) {
        Question q=getQuestion(questionId); QuestionVersion v=getVersion(questionId, number);
        assertVersion(q.version(), request.expectedQuestionVersion()); assertVersion(v.version(), request.expectedVersion());
        v.publish(clock.instant()); versions.saveAndFlush(v); q.setCurrentVersion(v.id(), QuestionStatus.PUBLISHED, clock.instant()); questions.saveAndFlush(q); return v;
    }
    private static void assertVersion(long actual, long expected) { if(actual != expected) throw new OptimisticLockException("Stale resource version"); }
    private static QuestionVersion.Content content(ContentRequest c) { return new QuestionVersion.Content(c.title(),c.prompt(),c.constraintsText(),c.examples(),c.supportedLanguages(),c.visibleTests(),c.hiddenTests(),c.scoringRules(),c.executionLimits()); }
}
