package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AssessmentVersionChallengeRepository extends JpaRepository<AssessmentVersionChallenge, AssessmentVersionChallengeId> { List<AssessmentVersionChallenge> findByAssessmentVersionIdOrderByPositionAsc(UUID assessmentVersionId); void deleteByAssessmentVersionId(UUID assessmentVersionId); }
