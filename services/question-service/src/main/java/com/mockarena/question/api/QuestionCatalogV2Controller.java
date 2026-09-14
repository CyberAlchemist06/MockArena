package com.mockarena.question.api;

import com.mockarena.question.application.QuestionVersionCatalogV2Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v2/question-versions")
public class QuestionCatalogV2Controller {
    private final QuestionVersionCatalogV2Service catalog;
    public QuestionCatalogV2Controller(QuestionVersionCatalogV2Service catalog) { this.catalog = catalog; }
    @PostMapping("/resolve")
    public QuestionCatalogV2Dtos.ResolveResponse resolve(@Valid @RequestBody QuestionCatalogV2Dtos.ResolveRequest request) {
        return catalog.resolve(request);
    }
}
