package com.mockarena.assessment.infrastructure.challenge;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestChallengeVersionCatalogClientTest {
    @Test
    void usesTheV2ManifestContractAndDeserializesOnlySafeRoutingMetadata() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://challenge-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestChallengeVersionCatalogClient client = new RestChallengeVersionCatalogClient(builder.build());
        UUID challengeId = UUID.randomUUID(), versionId = UUID.randomUUID(), questionId = UUID.randomUUID(), questionVersionId = UUID.randomUUID();
        server.expect(once(), requestTo("http://challenge-service/internal/v2/challenge-versions/manifests"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.challengeVersionIds[0]").value(versionId.toString()))
                .andRespond(withSuccess("""
                        {"entries":[{"challengeId":"%s","challengeVersionId":"%s","versionNumber":1,
                        "questions":[{"position":1,"questionId":"%s","questionVersionId":"%s","questionTypeCode":"MCQ"}]}]}
                        """.formatted(challengeId, versionId, questionId, questionVersionId), MediaType.APPLICATION_JSON));

        List<ChallengeVersionManifest> result = client.resolveManifests(List.of(versionId));

        assertThat(result).containsExactly(new ChallengeVersionManifest(challengeId, versionId, 1,
                List.of(new ChallengeQuestionReference(1, questionId, questionVersionId, "MCQ"))));
        server.verify();
    }
}
