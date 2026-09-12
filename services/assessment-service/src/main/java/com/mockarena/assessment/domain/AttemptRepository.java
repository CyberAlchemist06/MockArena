package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttemptRepository extends JpaRepository<Attempt, UUID> { Optional<Attempt> findByCandidateUserIdAndAssessmentVersionIdAndStatus(UUID candidateUserId, UUID assessmentVersionId, AttemptStatus status); long countByCandidateUserIdAndAssessmentVersionId(UUID candidateUserId, UUID assessmentVersionId); }
