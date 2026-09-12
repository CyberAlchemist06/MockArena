package com.mockarena.challenge.api;

import com.mockarena.challenge.application.ChallengeVersionResolutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v2/challenge-versions")
public class ChallengeVersionManifestInternalController {
    private final ChallengeVersionResolutionService versions;

    public ChallengeVersionManifestInternalController(ChallengeVersionResolutionService versions) {
        this.versions = versions;
    }

    @PostMapping("/manifests")
    public ChallengeVersionInternalDtos.ManifestResponse manifests(
            @Valid @RequestBody ChallengeVersionInternalDtos.ManifestRequest request) {
        return new ChallengeVersionInternalDtos.ManifestResponse(versions.resolveManifests(request.challengeVersionIds()));
    }
}
