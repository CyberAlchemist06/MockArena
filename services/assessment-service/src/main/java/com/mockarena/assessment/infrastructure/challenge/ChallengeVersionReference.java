package com.mockarena.assessment.infrastructure.challenge;
import java.util.UUID;
public record ChallengeVersionReference(UUID challengeId, UUID challengeVersionId, int challengeVersionNumber, String status) { }
