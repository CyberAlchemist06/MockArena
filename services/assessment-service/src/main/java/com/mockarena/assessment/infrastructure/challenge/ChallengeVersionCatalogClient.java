package com.mockarena.assessment.infrastructure.challenge;
import java.util.*;
public interface ChallengeVersionCatalogClient {
    List<ChallengeVersionReference> resolve(List<UUID> challengeVersionIds);
    List<ChallengeVersionManifest> resolveManifests(List<UUID> challengeVersionIds);
}
