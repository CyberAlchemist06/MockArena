package com.mockarena.challenge.domain;
import java.util.List;
public record RuleBasedSelection(List<String> tagsAll, List<String> difficulties, String supportedLanguage, int requestedQuestionCount) { }
