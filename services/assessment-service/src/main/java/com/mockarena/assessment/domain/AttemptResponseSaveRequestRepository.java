package com.mockarena.assessment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AttemptResponseSaveRequestRepository extends JpaRepository<AttemptResponseSaveRequest, UUID> {
    Optional<AttemptResponseSaveRequest> findByAttemptIdAndIdempotencyKey(UUID attemptId, String idempotencyKey);
    Optional<AttemptResponseSaveRequest> findByAttemptIdAndClientMutationId(UUID attemptId, UUID clientMutationId);
}
