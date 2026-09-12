package com.mockarena.assessment.infrastructure.question;
import com.fasterxml.jackson.databind.JsonNode; import java.util.*; public record QuestionCandidateContent(UUID questionId,UUID questionVersionId,String questionTypeCode,String title,String stem,List<Option> options,String constraints,JsonNode examples,JsonNode programmingLanguages){public record Option(String id,String text){}}
