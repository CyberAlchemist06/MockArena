package com.mockarena.assessment.api;
import com.mockarena.assessment.application.*; import com.mockarena.assessment.api.AssessmentDtos.*; import com.mockarena.assessment.security.CurrentAuthenticatedUser; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/attempts") public class AttemptController {
    private final AttemptContentApplicationService content; private final AttemptResponseApplicationService responses; private final AttemptSubmissionApplicationService submissions; private final CurrentAuthenticatedUser current;
    public AttemptController(AttemptContentApplicationService content, AttemptResponseApplicationService responses, AttemptSubmissionApplicationService submissions, CurrentAuthenticatedUser current){this.content=content;this.responses=responses;this.submissions=submissions;this.current=current;}
    @GetMapping("/{attemptId}/content") public List<AttemptContentApplicationService.Item> content(@PathVariable UUID attemptId){return content.content(attemptId,current.userId());}
    @PutMapping("/{attemptId}/responses/{globalPosition}") public SavedAttemptResponse save(@PathVariable UUID attemptId, @PathVariable int globalPosition, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @Valid @RequestBody SaveAttemptResponseRequest request){return responses.save(attemptId,globalPosition,current.userId(),idempotencyKey,request);}
    @GetMapping("/{attemptId}/responses") public List<SavedAttemptResponse> responses(@PathVariable UUID attemptId){return responses.savedResponses(attemptId,current.userId());}
    @PostMapping("/{attemptId}/submit") public AttemptSubmissionApplicationService.Submission submit(@PathVariable UUID attemptId,@RequestHeader("Idempotency-Key") String key){return submissions.submit(attemptId,current.userId(),key);}
}
