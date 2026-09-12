package com.mockarena.question;

import com.mockarena.question.bootstrap.DevelopmentQuestionSeed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"question.seed.development-data=true", "question.security.enabled=false"})
@ActiveProfiles("dev")
@Testcontainers
class DevelopmentQuestionSeedIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DevelopmentQuestionSeed seed;

    @Test void createsPublishedCodingAndMcqQuestionsAndIsIdempotent() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from question.questions where lifecycle_status = 'PUBLISHED'", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("select count(*) from question.question_versions where status = 'PUBLISHED' and version_number = 1", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("select count(*) from question.questions q join question.question_versions qv on q.current_version_id = qv.id", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("select count(*) from question.question_versions where question_type = 'CODING'", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("select count(*) from question.question_versions where question_type = 'MCQ'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("select title from question.question_versions order by title", String.class))
                .containsExactlyInAnyOrderElementsOf(List.of(
                        "Two Sum", "Move Zeroes", "Best Time to Buy and Sell Stock", "Longest Subarray With Sum K",
                        "Product of Array Except Self", "Valid Anagram", "Longest Substring Without Repeating Characters",
                        "Binary Search in Sorted Array", "First and Last Position", "Maximum Depth of Binary Tree",
                        "Binary Tree Level Order Traversal", "Number of Islands", "BST Traversal Order", "Fast Membership Lookup"));

        seed.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("select count(*) from question.questions", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("select count(*) from question.question_versions", Integer.class)).isEqualTo(14);
    }
}
