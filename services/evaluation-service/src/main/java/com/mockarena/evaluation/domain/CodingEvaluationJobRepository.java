package com.mockarena.evaluation.domain;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface CodingEvaluationJobRepository extends JpaRepository<CodingEvaluationJob,UUID>{
 Optional<CodingEvaluationJob> findByAttemptIdAndGlobalPositionAndQuestionVersionIdAndSubmittedResponseFingerprint(UUID attemptId,int position,UUID versionId,String fingerprint);
 @Query(value="select * from evaluation.coding_evaluation_jobs where (status in ('RECEIVED','RETRY_WAIT') and next_attempt_at <= :now) or (status='RESOLVING_SPEC' and lease_expires_at < :now) order by created_at for update skip locked limit :limit",nativeQuery=true) List<CodingEvaluationJob> claimable(@Param("now") Instant now,@Param("limit") int limit);
 @Query(value="select * from evaluation.coding_evaluation_jobs where status='READY_FOR_RUNNER' or (status='RUNNING' and lease_expires_at < :now) order by created_at for update skip locked limit :limit",nativeQuery=true) List<CodingEvaluationJob> runnable(@Param("now") Instant now,@Param("limit") int limit);
}
