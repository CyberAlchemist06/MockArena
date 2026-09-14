package com.mockarena.assessment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AttemptItemResponseRepository extends JpaRepository<AttemptItemResponse, AttemptItemResponseId> {
    Optional<AttemptItemResponse> findByAttemptIdAndGlobalPosition(UUID attemptId, int globalPosition);
    List<AttemptItemResponse> findByAttemptIdOrderByGlobalPositionAsc(UUID attemptId);
}
