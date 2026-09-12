package com.mockarena.question.api;
import com.fasterxml.jackson.databind.JsonNode; import jakarta.validation.constraints.*; import java.util.*;
public final class QuestionCandidateContentDtos {
 private QuestionCandidateContentDtos(){}
 public record Request(@NotEmpty List<@NotNull UUID> questionVersionIds){}
 public record Option(String id,String text){}
 public record Entry(UUID questionId,UUID questionVersionId,String questionTypeCode,String title,String stem,List<Option> options,String constraints,JsonNode examples,JsonNode programmingLanguages){}
 public record Response(List<Entry> entries){}
}
