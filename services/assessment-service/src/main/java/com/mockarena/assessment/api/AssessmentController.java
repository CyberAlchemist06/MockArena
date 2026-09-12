package com.mockarena.assessment.api;

import com.mockarena.assessment.application.AssessmentApplicationService;
import com.mockarena.assessment.security.CurrentAuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController @RequestMapping("/api/v1/assessments")
public class AssessmentController {
    private final AssessmentApplicationService assessments; private final com.mockarena.assessment.application.AttemptApplicationService attempts; private final CurrentAuthenticatedUser currentUser;
    public AssessmentController(AssessmentApplicationService assessments, com.mockarena.assessment.application.AttemptApplicationService attempts, CurrentAuthenticatedUser currentUser) { this.assessments = assessments; this.attempts = attempts; this.currentUser = currentUser; }
    @PostMapping public ResponseEntity<AssessmentDtos.AssessmentVersionResponse> create(@Valid @RequestBody AssessmentDtos.CreateAssessmentRequest request) { AssessmentDtos.AssessmentVersionResponse response = assessments.create(request, currentUser.userId()); return ResponseEntity.created(URI.create("/api/v1/assessments/" + response.assessmentId())).body(response); }
    @GetMapping("/{assessmentId}") public AssessmentDtos.AssessmentResponse assessment(@PathVariable UUID assessmentId) { return assessments.assessment(assessmentId); }
    @GetMapping("/{assessmentId}/versions") public List<AssessmentDtos.AssessmentVersionResponse> versions(@PathVariable UUID assessmentId) { return assessments.versions(assessmentId); }
    @GetMapping("/{assessmentId}/versions/{versionNumber}") public AssessmentDtos.AssessmentVersionResponse version(@PathVariable UUID assessmentId, @PathVariable int versionNumber) { return assessments.version(assessmentId, versionNumber); }
    @PutMapping("/{assessmentId}/versions/{versionNumber}") public AssessmentDtos.AssessmentVersionResponse update(@PathVariable UUID assessmentId, @PathVariable int versionNumber, @Valid @RequestBody AssessmentDtos.UpdateAssessmentVersionRequest request) { return assessments.update(assessmentId, versionNumber, request); }
    @PostMapping("/{assessmentId}/versions") @ResponseStatus(HttpStatus.CREATED) public AssessmentDtos.AssessmentVersionResponse revise(@PathVariable UUID assessmentId, @Valid @RequestBody AssessmentDtos.CreateRevisionRequest request) { return assessments.revise(assessmentId, request); }
    @PostMapping("/{assessmentId}/versions/{versionNumber}/publish") public AssessmentDtos.AssessmentVersionResponse publish(@PathVariable UUID assessmentId, @PathVariable int versionNumber, @Valid @RequestBody AssessmentDtos.PublishAssessmentVersionRequest request) { return assessments.publish(assessmentId, versionNumber, request); }
    @PostMapping("/{assessmentId}/versions/{versionNumber}/retire") public AssessmentDtos.AssessmentVersionResponse retire(@PathVariable UUID assessmentId, @PathVariable int versionNumber, @Valid @RequestBody AssessmentDtos.RetireAssessmentVersionRequest request) { return assessments.retire(assessmentId, versionNumber, request); }
    @PostMapping("/{assessmentId}/versions/{versionNumber}/close") public AssessmentDtos.AssessmentVersionResponse closeVersion(@PathVariable UUID assessmentId, @PathVariable int versionNumber, @Valid @RequestBody AssessmentDtos.CloseAssessmentVersionRequest request) { return assessments.closeVersion(assessmentId, versionNumber, request); }
    @PostMapping("/{assessmentId}/close") public AssessmentDtos.AssessmentResponse close(@PathVariable UUID assessmentId, @Valid @RequestBody AssessmentDtos.CloseAssessmentRequest request) { return assessments.close(assessmentId, request); }
    @PostMapping("/{assessmentId}/versions/{versionNumber}/attempts") @ResponseStatus(HttpStatus.CREATED)
    public AssessmentDtos.AttemptResponse startAttempt(@PathVariable UUID assessmentId, @PathVariable int versionNumber, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return AssessmentDtos.attempt(attempts.start(assessmentId, versionNumber, currentUser.userId(), idempotencyKey));
    }
}
