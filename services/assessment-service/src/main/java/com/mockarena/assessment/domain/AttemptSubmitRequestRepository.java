package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttemptSubmitRequestRepository extends JpaRepository<AttemptSubmitRequest,UUID>{ Optional<AttemptSubmitRequest> findByAttemptIdAndIdempotencyKey(UUID attemptId,String key); }
