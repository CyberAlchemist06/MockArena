package com.mockarena.challenge.domain;
import java.util.List;
public record RuleBasedSelection(List<TaxonomyAssignment> taxonomyAll, List<String> questionTypeCodes,
                                 List<DifficultyProfile> difficultyProfiles, List<String> contentLocales,
                                 List<String> programmingLanguages, int requestedQuestionCount) { }
