package com.mockarena.challenge.question;
import com.mockarena.challenge.domain.RuleBasedSelection;
import java.util.List;
public interface QuestionCatalogClient { List<QuestionCatalogEntry> resolve(RuleBasedSelection selection); }
