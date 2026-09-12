package com.mockarena.assessment.infrastructure.challenge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.*;

@Component
public class RestChallengeVersionCatalogClient implements ChallengeVersionCatalogClient {
    private final RestClient client;
    public RestChallengeVersionCatalogClient(@Value("${challenge-service.base-url}") String baseUrl) { this(RestClient.builder().baseUrl(baseUrl).build()); }
    RestChallengeVersionCatalogClient(RestClient client) { this.client = client; }
    @Override public List<ChallengeVersionReference> resolve(List<UUID> challengeVersionIds) {
        try {
            ResolveResponse response = client.post().uri("/internal/v1/challenge-versions/resolve").body(new ResolveRequest(challengeVersionIds)).retrieve().body(ResolveResponse.class);
            if (response == null || response.entries() == null || response.entries().size() != challengeVersionIds.size()) throw new ChallengeVersionNotComposableException();
            List<ChallengeVersionReference> references = response.entries().stream().map(entry -> new ChallengeVersionReference(entry.challengeId(), entry.challengeVersionId(), entry.versionNumber(), entry.status())).toList();
            for (int index = 0; index < references.size(); index++) if (!challengeVersionIds.get(index).equals(references.get(index).challengeVersionId()) || !"PUBLISHED".equals(references.get(index).status())) throw new ChallengeVersionNotComposableException();
            return references;
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 404 || status.value() == 422) throw new ChallengeVersionNotComposableException();
            throw new ChallengeServiceUnavailableException(exception);
        } catch (ChallengeVersionNotComposableException exception) { throw exception;
        } catch (RestClientException exception) { throw new ChallengeServiceUnavailableException(exception); }
    }
    @Override public List<ChallengeVersionManifest> resolveManifests(List<UUID> challengeVersionIds) {
        try {
            ManifestResponse response = client.post().uri("/internal/v2/challenge-versions/manifests")
                    .body(new ResolveRequest(challengeVersionIds)).retrieve().body(ManifestResponse.class);
            if (response == null || response.entries() == null || response.entries().size() != challengeVersionIds.size()) throw new ChallengeVersionNotComposableException();
            List<ChallengeVersionManifest> manifests = response.entries().stream().map(entry -> new ChallengeVersionManifest(
                    entry.challengeId(), entry.challengeVersionId(), entry.versionNumber(), entry.questions().stream()
                            .map(question -> new ChallengeQuestionReference(question.position(), question.questionId(), question.questionVersionId(), question.questionTypeCode())).toList())).toList();
            for (int index = 0; index < manifests.size(); index++) {
                ChallengeVersionManifest manifest = manifests.get(index);
                if (!challengeVersionIds.get(index).equals(manifest.challengeVersionId()) || manifest.questions().isEmpty()
                        || manifest.questions().stream().anyMatch(question -> question.position() < 1 || question.questionId() == null
                        || question.questionVersionId() == null || question.questionTypeCode() == null || question.questionTypeCode().isBlank())) {
                    throw new ChallengeVersionNotComposableException();
                }
            }
            return manifests;
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 404 || status.value() == 422) throw new ChallengeVersionNotComposableException();
            throw new ChallengeServiceUnavailableException(exception);
        } catch (ChallengeVersionNotComposableException exception) { throw exception;
        } catch (RestClientException exception) { throw new ChallengeServiceUnavailableException(exception); }
    }
    record ResolveRequest(List<UUID> challengeVersionIds) { }
    record ResolveResponse(List<Entry> entries) { }
    record Entry(UUID challengeId, UUID challengeVersionId, int versionNumber, String status) { }
    record ManifestResponse(List<ManifestEntry> entries) { }
    record ManifestEntry(UUID challengeId, UUID challengeVersionId, int versionNumber, List<ManifestQuestion> questions) { }
    record ManifestQuestion(int position, UUID questionId, UUID questionVersionId, String questionTypeCode) { }
}
