package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    Optional<Attempt> findByCandidateUserIdAndAssessmentVersionIdAndStatus(UUID candidateUserId, UUID assessmentVersionId, AttemptStatus status);
    long countByCandidateUserIdAndAssessmentVersionId(UUID candidateUserId, UUID assessmentVersionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from Attempt attempt where attempt.id = :attemptId")
    Optional<Attempt> findByIdForUpdate(@Param("attemptId") UUID attemptId);
}
