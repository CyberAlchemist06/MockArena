package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttemptStartRequestRepository extends JpaRepository<AttemptStartRequest, UUID> { Optional<AttemptStartRequest> findByCandidateUserIdAndIdempotencyKey(UUID candidateUserId, String idempotencyKey); }
