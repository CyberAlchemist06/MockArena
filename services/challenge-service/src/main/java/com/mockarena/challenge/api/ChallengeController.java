package com.mockarena.challenge.api;

import com.mockarena.challenge.application.ChallengeApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {
    private final ChallengeApplicationService challenges;
    public ChallengeController(ChallengeApplicationService challenges) { this.challenges = challenges; }
    @PostMapping
    public ResponseEntity<ChallengeDtos.ChallengeResponse> create(@Valid @RequestBody ChallengeDtos.CreateChallengeRequest request) {
        ChallengeDtos.ChallengeResponse response = challenges.create(request);
        return ResponseEntity.created(URI.create("/api/v1/challenges/" + response.challengeId())).body(response);
    }
}
