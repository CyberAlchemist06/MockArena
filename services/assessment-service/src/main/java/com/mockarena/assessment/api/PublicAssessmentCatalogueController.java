package com.mockarena.assessment.api;

import com.mockarena.assessment.application.PublicAssessmentCatalogueService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/public/assessments")
public class PublicAssessmentCatalogueController {
    private final PublicAssessmentCatalogueService catalogue;
    public PublicAssessmentCatalogueController(PublicAssessmentCatalogueService catalogue) { this.catalogue = catalogue; }
    @GetMapping public PublicAssessmentDtos.CataloguePage list(@RequestParam(required = false) String q, @RequestParam(required = false) String assessmentTypeCode, @RequestParam(required = false) String availability, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String cursor) { return catalogue.list(q, assessmentTypeCode, availability, pageSize, cursor); }
    @GetMapping("/{assessmentId}") public PublicAssessmentDtos.AssessmentDetail detail(@PathVariable UUID assessmentId) { return catalogue.detail(assessmentId); }
}
