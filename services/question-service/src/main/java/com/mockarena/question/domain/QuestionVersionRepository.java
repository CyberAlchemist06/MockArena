package com.mockarena.question.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, UUID> {
    Optional<QuestionVersion> findByQuestionIdAndVersionNumber(UUID questionId, int versionNumber);
    Optional<QuestionVersion> findTopByQuestionIdOrderByVersionNumberDesc(UUID questionId);
    List<QuestionVersion> findByQuestionIdOrderByVersionNumberDesc(UUID questionId);
}
