package com.mockarena.question.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockarena.question.api.QuestionDtos;
import com.mockarena.question.application.QuestionApplicationService;
import com.mockarena.question.domain.Difficulty;
import com.mockarena.question.domain.Question;
import com.mockarena.question.domain.QuestionRepository;
import com.mockarena.question.domain.QuestionVersion;
import com.mockarena.question.domain.QuestionVersionRepository;
import com.mockarena.question.domain.QuestionVersionStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "question.seed.development-data", havingValue = "true")
public class DevelopmentQuestionSeed implements ApplicationRunner {
    static final UUID DEVELOPMENT_SEED_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final QuestionApplicationService questions;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public DevelopmentQuestionSeed(QuestionApplicationService questions, QuestionRepository questionRepository,
                                   QuestionVersionRepository versionRepository, ObjectMapper objectMapper) {
        this.questions = questions;
        this.questionRepository = questionRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    @Override public void run(ApplicationArguments args) { seeds().forEach(this::seed); }

    private void seed(SeedQuestion seed) {
        Question existing = questionRepository.findByOwnerUserId(DEVELOPMENT_SEED_OWNER_ID).stream()
                .filter(question -> versionRepository.findByQuestionIdOrderByVersionNumberDesc(question.id()).stream().anyMatch(version -> version.title().equals(seed.title())))
                .findFirst().orElse(null);
        if (existing == null) {
            QuestionVersion created = questions.create(new QuestionDtos.CreateQuestionRequest(DEVELOPMENT_SEED_OWNER_ID, content(seed)));
            questions.publish(created.questionId(), created.versionNumber(), new QuestionDtos.PublishVersionRequest(0, 0));
            return;
        }
        QuestionVersion version = versionRepository.findByQuestionIdOrderByVersionNumberDesc(existing.id()).stream()
                .filter(candidate -> candidate.title().equals(seed.title())).findFirst().orElseThrow();
        if (version.status() == QuestionVersionStatus.DRAFT) {
            questions.publish(existing.id(), version.versionNumber(), new QuestionDtos.PublishVersionRequest(existing.version(), version.version()));
        }
    }

    private QuestionDtos.ContentRequest content(SeedQuestion seed) {
        return new QuestionDtos.ContentRequest(seed.title(), seed.tags(), seed.difficulty(), seed.prompt(), seed.constraints(),
                cases(seed.exampleInput(), seed.exampleOutput()), array("JAVA"), cases(seed.visibleInput(), seed.visibleOutput()),
                cases(seed.hiddenInput(), seed.hiddenOutput()), object(Map.of("maxPoints", 100, "strategy", "ALL_OR_NOTHING")),
                object(Map.of("timeMs", 1000, "memoryMb", 256)));
    }

    private JsonNode array(String value) { return object(List.of(value)); }
    private JsonNode cases(String input, String output) { return object(List.of(Map.of("input", input, "output", output))); }
    private JsonNode object(Object value) { return objectMapper.valueToTree(value); }

    static List<SeedQuestion> seeds() {
        return List.of(
                q("Two Sum", "arrays,hashing", Difficulty.EASY, "Return indices of two numbers whose sum equals target.", "One zero-based index pair exists.", "nums=[2,7,11,15], target=9", "[0,1]", "nums=[3,2,4], target=6", "[1,2]"),
                q("Move Zeroes", "arrays,two-pointer", Difficulty.EASY, "Move zeroes to the end while preserving non-zero order.", "Modify the sequence in place.", "nums=[0,1,0,3,12]", "[1,3,12,0,0]", "nums=[0,0,1]", "[1,0,0]"),
                q("Best Time to Buy and Sell Stock", "arrays,greedy", Difficulty.EASY, "Find the greatest profit from one buy before one sell.", "Return zero if no profit is possible.", "prices=[7,1,5,3,6,4]", "5", "prices=[7,6,4,3,1]", "0"),
                q("Longest Subarray With Sum K", "arrays,sliding-window", Difficulty.MEDIUM, "Return the longest contiguous subarray with sum k.", "Input values are non-negative.", "nums=[1,2,1,0,1,1,0], k=4", "4", "nums=[1,2,3], k=7", "0"),
                q("Product of Array Except Self", "arrays,prefix-sum", Difficulty.MEDIUM, "Return products of all values except the value at each position.", "Do not use division.", "nums=[1,2,3,4]", "[24,12,8,6]", "nums=[-1,1,0,-3,3]", "[0,0,9,0,0]"),
                q("Valid Anagram", "strings,hashing", Difficulty.EASY, "Determine whether two strings have identical character frequencies.", "Strings contain lowercase letters.", "s=anagram, t=nagaram", "true", "s=rat, t=car", "false"),
                q("Longest Substring Without Repeating Characters", "strings,sliding-window", Difficulty.MEDIUM, "Return the longest substring length without repeated characters.", "The input can be empty.", "s=abcabcbb", "3", "s=bbbbb", "1"),
                q("Binary Search in Sorted Array", "binary-search,arrays", Difficulty.EASY, "Return target's index in a sorted array or -1.", "Values are distinct and sorted ascending.", "nums=[-1,0,3,5,9,12], target=9", "4", "nums=[-1,0,3,5,9,12], target=2", "-1"),
                q("First and Last Position", "binary-search,arrays", Difficulty.MEDIUM, "Find target's first and last position in a sorted array.", "Return [-1,-1] when absent.", "nums=[5,7,7,8,8,10], target=8", "[3,4]", "nums=[5,7,7,8,8,10], target=6", "[-1,-1]"),
                q("Maximum Depth of Binary Tree", "trees,dfs", Difficulty.EASY, "Return the maximum root-to-leaf node depth.", "Use level-order null markers for trees.", "root=[3,9,20,null,null,15,7]", "3", "root=[]", "0"),
                q("Binary Tree Level Order Traversal", "trees,bfs", Difficulty.MEDIUM, "Return node values grouped by tree level.", "Use level-order null markers for trees.", "root=[3,9,20,null,null,15,7]", "[[3],[9,20],[15,7]]", "root=[]", "[]"),
                q("Number of Islands", "graphs,bfs,dfs", Difficulty.MEDIUM, "Count islands connected horizontally or vertically in a grid.", "Grid cells are 0 or 1.", "grid=[[1,1,0],[1,0,0],[0,0,1]]", "2", "grid=[[0,0],[0,0]]", "0"));
    }

    private static SeedQuestion q(String title, String tags, Difficulty difficulty, String prompt, String constraints, String visibleInput, String visibleOutput, String hiddenInput, String hiddenOutput) {
        return new SeedQuestion(title, List.of(tags.split(",")), difficulty, prompt, constraints, visibleInput, visibleOutput, hiddenInput, hiddenOutput);
    }

    record SeedQuestion(String title, List<String> tags, Difficulty difficulty, String prompt, String constraints,
                        String visibleInput, String visibleOutput, String hiddenInput, String hiddenOutput) {
        String exampleInput() { return visibleInput; }
        String exampleOutput() { return visibleOutput; }
    }
}
