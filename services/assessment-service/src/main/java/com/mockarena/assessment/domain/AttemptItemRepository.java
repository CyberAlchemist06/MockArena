package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttemptItemRepository extends JpaRepository<AttemptItem, AttemptItemId> { List<AttemptItem> findByAttemptIdOrderByGlobalPositionAsc(UUID attemptId); }
