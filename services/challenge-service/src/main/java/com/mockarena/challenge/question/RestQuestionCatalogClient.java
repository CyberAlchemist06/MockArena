package com.mockarena.challenge.question;

import com.mockarena.challenge.domain.RuleBasedSelection;
import com.mockarena.challenge.domain.SelectionGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.UUID;

@Component
public class RestQuestionCatalogClient implements QuestionCatalogClient {
    private final RestClient client;
    public RestQuestionCatalogClient(@Value("${question-service.base-url}") String baseUrl) { client = RestClient.builder().baseUrl(baseUrl).build(); }
    @Override public List<QuestionCatalogEntry> resolve(RuleBasedSelection selection) {
        return resolvePage(new SelectionGroup(selection.taxonomyAll(), selection.questionTypeCodes(), selection.difficultyProfiles(), selection.contentLocales(), selection.programmingLanguages(), selection.requestedQuestionCount()), null).entries();
    }
    @Override public CatalogPage resolvePage(SelectionGroup selection, String cursor) {
        try {
            CatalogResponse response = client.post().uri("/internal/v2/question-versions/resolve")
                    .body(new CatalogRequest(selection.taxonomyAll(), selection.questionTypeCodes(), selection.difficultyProfiles(), selection.contentLocales(), selection.programmingLanguages(), 100, cursor))
                    .retrieve().body(CatalogResponse.class);
            if (response == null || response.entries() == null) throw new QuestionCatalogUnavailableException();
            return new CatalogPage(response.entries().stream().map(entry -> new QuestionCatalogEntry(entry.questionId(), entry.questionVersionId(), entry.questionTypeCode())).toList(), response.nextCursor());
        } catch (RestClientException exception) { throw new QuestionCatalogUnavailableException(exception); }
    }
    record CatalogRequest(List<com.mockarena.challenge.domain.TaxonomyAssignment> taxonomyAll, List<String> questionTypeCodes,
                          List<com.mockarena.challenge.domain.DifficultyProfile> difficultyProfiles, List<String> contentLocales,
                          List<String> programmingLanguages, int limit, String cursor) { }
    record CatalogResponse(List<CatalogEntryResponse> entries, String nextCursor) { }
    record CatalogEntryResponse(UUID questionId, UUID questionVersionId, int versionNumber, String questionTypeCode) { }
}
