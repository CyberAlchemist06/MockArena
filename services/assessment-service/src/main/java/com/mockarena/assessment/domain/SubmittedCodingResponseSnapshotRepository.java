package com.mockarena.assessment.domain; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SubmittedCodingResponseSnapshotRepository extends JpaRepository<SubmittedCodingResponseSnapshot,SubmittedCodingResponseSnapshotId>{ List<SubmittedCodingResponseSnapshot> findByAttemptIdOrderByGlobalPositionAsc(UUID attemptId); }
