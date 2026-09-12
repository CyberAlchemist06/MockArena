package com.mockarena.question.application;

import com.mockarena.question.api.QuestionCatalogV2Dtos.*;
import com.mockarena.question.infrastructure.persistence.QuestionVersionCatalogV2Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QuestionVersionCatalogV2Service {
    private final QuestionVersionCatalogV2Repository repository;
    public QuestionVersionCatalogV2Service(QuestionVersionCatalogV2Repository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Entry> resolve(ResolveRequest request) { return repository.resolve(request); }
}
