package com.mockarena.assessment.config;

import com.mockarena.assessment.infrastructure.challenge.RestChallengeVersionCatalogClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ChallengeServiceClientConfiguration {
    @Bean
    RestClient challengeServiceRestClient(@Value("${challenge-service.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    RestChallengeVersionCatalogClient restChallengeVersionCatalogClient(
            @Qualifier("challengeServiceRestClient") RestClient challengeServiceRestClient) {
        return new RestChallengeVersionCatalogClient(challengeServiceRestClient);
    }
}
