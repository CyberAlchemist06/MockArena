package com.mockarena.assessment.application;

import com.mockarena.assessment.domain.*;
import org.slf4j.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "assessment.catalogue.backfill", name = "enabled", havingValue = "true")
public class PublicAssessmentCatalogueBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PublicAssessmentCatalogueBackfill.class);
    private final AssessmentVersionRepository versions;
    private final AssessmentVersionChallengeRepository manifests;
    private final PublicAssessmentCatalogueProjectionService projections;
    public PublicAssessmentCatalogueBackfill(AssessmentVersionRepository versions, AssessmentVersionChallengeRepository manifests, PublicAssessmentCatalogueProjectionService projections) { this.versions = versions; this.manifests = manifests; this.projections = projections; }
    @Override public void run(ApplicationArguments args) {
        var missing = versions.findCurrentPublicPublishedWithoutCatalogueProjection();
        log.info("Public assessment catalogue backfill found {} missing projection(s)", missing.size());
        for (AssessmentVersion version : missing) try {
            projections.create(version, manifests.findByAssessmentVersionIdOrderByPositionAsc(version.id()), version.updatedAt());
            log.info("Created public catalogue projection for assessmentVersionId={}", version.id());
        } catch (RuntimeException exception) {
            log.error("Could not create public catalogue projection for assessmentVersionId={}; continuing", version.id(), exception);
        }
    }
}
