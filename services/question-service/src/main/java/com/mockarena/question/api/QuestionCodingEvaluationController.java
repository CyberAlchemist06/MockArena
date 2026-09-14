package com.mockarena.question.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mockarena.question.application.QuestionCodingEvaluationDataService;
import com.mockarena.question.domain.CodingEvaluationData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Protected workload endpoint. It is never a candidate or public API. */
@RestController
@RequestMapping("/internal/v1/question-versions")
public class QuestionCodingEvaluationController {
    private final QuestionCodingEvaluationDataService service;
    public QuestionCodingEvaluationController(QuestionCodingEvaluationDataService service) { this.service = service; }

    @PostMapping("/coding-evaluation-data")
    public Response data(@Valid @RequestBody Request request) {
        return new Response(service.resolve(request.questionVersionIds()).stream().map(Entry::from).toList());
    }

    public record Request(@NotEmpty List<UUID> questionVersionIds) { }
    public record Entry(UUID questionVersionId, String questionTypeCode, String runtimeProfileId,
                        JsonNode allowedProgrammingLanguages, JsonNode executionLimits,
                        JsonNode scoringPolicy, JsonNode testSpecification) {
        static Entry from(CodingEvaluationData data) {
            return new Entry(data.questionVersionId(), data.questionTypeCode(), data.runtimeProfileId(),
                    data.allowedProgrammingLanguages(), data.executionLimits(), data.scoringPolicy(), data.testSpecification());
        }
    }
    public record Response(List<Entry> entries) { }
}
