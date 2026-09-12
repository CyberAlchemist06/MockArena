package com.mockarena.question.api;

import com.mockarena.question.application.QuestionVersionCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/question-versions")
public class QuestionCatalogController {
    private final QuestionVersionCatalogService catalog;

    public QuestionCatalogController(QuestionVersionCatalogService catalog) { this.catalog = catalog; }

    @PostMapping("/resolve")
    public QuestionCatalogDtos.ResolveQuestionVersionsResponse resolve(@Valid @RequestBody QuestionCatalogDtos.ResolveQuestionVersionsRequest request) {
        return QuestionCatalogDtos.ResolveQuestionVersionsResponse.from(catalog.resolve(request));
    }
}
