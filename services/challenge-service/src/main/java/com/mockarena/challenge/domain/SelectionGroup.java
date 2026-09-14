package com.mockarena.challenge.domain;

import java.util.List;

/** Draft-only, generic catalogue filter bucket. Product policy is applied by the application layer. */
public record SelectionGroup(List<TaxonomyAssignment> taxonomyAll, List<String> questionTypeCodes,
                             List<DifficultyProfile> difficultyProfiles, List<String> contentLocales,
                             List<String> programmingLanguages, int requestedQuestionCount) { }
