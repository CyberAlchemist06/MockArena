package com.mockarena.assessment.api;

import com.mockarena.assessment.api.AssessmentDtos.InternalEvaluationResponse;
import com.mockarena.assessment.application.AttemptEvaluationService;
import com.mockarena.assessment.domain.AttemptResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Operational recovery endpoint. It is deliberately outside the browser-facing API namespace. */
@RestController
@RequestMapping("/internal/v1/attempts")
public class InternalAttemptEvaluationController {
    private final AttemptEvaluationService evaluation;

    public InternalAttemptEvaluationController(AttemptEvaluationService evaluation) { this.evaluation = evaluation; }

    @PostMapping("/{attemptId}/evaluate")
    public InternalEvaluationResponse evaluate(@PathVariable UUID attemptId) {
        AttemptResult result = evaluation.evaluate(attemptId);
        return new InternalEvaluationResponse(attemptId, result.evaluationStatus().name());
    }
}
