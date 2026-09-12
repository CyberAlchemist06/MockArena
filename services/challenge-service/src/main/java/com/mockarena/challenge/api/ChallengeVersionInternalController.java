package com.mockarena.challenge.api;

import com.mockarena.challenge.application.ChallengeVersionResolutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/challenge-versions")
public class ChallengeVersionInternalController {
    private final ChallengeVersionResolutionService versions;
    public ChallengeVersionInternalController(ChallengeVersionResolutionService versions) { this.versions = versions; }
    @PostMapping("/resolve")
    public ChallengeVersionInternalDtos.ResolveResponse resolve(@Valid @RequestBody ChallengeVersionInternalDtos.ResolveRequest request) {
        return new ChallengeVersionInternalDtos.ResolveResponse(versions.resolve(request.challengeVersionIds()));
    }
}
