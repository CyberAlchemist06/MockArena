package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AssessmentVersionRepository extends JpaRepository<AssessmentVersion, UUID> { Optional<AssessmentVersion> findByAssessmentIdAndVersionNumber(UUID assessmentId, int versionNumber); Optional<AssessmentVersion> findTopByAssessmentIdOrderByVersionNumberDesc(UUID assessmentId); List<AssessmentVersion> findByAssessmentIdOrderByVersionNumberDesc(UUID assessmentId); }
