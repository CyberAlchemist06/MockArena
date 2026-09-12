package com.mockarena.challenge.api;

import com.mockarena.challenge.application.ChallengeApplicationService;
import com.mockarena.challenge.security.CurrentAuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {
    private final ChallengeApplicationService challenges; private final CurrentAuthenticatedUser currentUser;
    public ChallengeController(ChallengeApplicationService challenges, CurrentAuthenticatedUser currentUser) { this.challenges = challenges; this.currentUser = currentUser; }
    @PostMapping
    public ResponseEntity<ChallengeDtos.ChallengeResponse> create(@Valid @RequestBody ChallengeDtos.CreateChallengeRequest request) {
        ChallengeDtos.ChallengeResponse response = challenges.create(request, currentUser.userId());
        return ResponseEntity.created(URI.create("/api/v1/challenges/" + response.challengeId())).body(response);
    }
    @PostMapping("/{challengeId}/versions/{versionNumber}/publish")
    public ChallengeDtos.ChallengeResponse publish(@PathVariable UUID challengeId, @PathVariable int versionNumber, @Valid @RequestBody ChallengeDtos.PublishChallengeVersionRequest request) {
        return challenges.publish(challengeId, versionNumber, request);
    }
    @PostMapping("/{challengeId}/versions/{versionNumber}/retire")
    public ChallengeDtos.ChallengeResponse retire(@PathVariable UUID challengeId, @PathVariable int versionNumber, @Valid @RequestBody ChallengeDtos.RetireChallengeVersionRequest request) {
        return challenges.retire(challengeId, versionNumber, request);
    }
}
