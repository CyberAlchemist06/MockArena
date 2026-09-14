package com.mockarena.challenge.question;
import com.mockarena.challenge.domain.RuleBasedSelection;
import com.mockarena.challenge.domain.SelectionGroup;
import java.util.List;
public interface QuestionCatalogClient {
    List<QuestionCatalogEntry> resolve(RuleBasedSelection selection);
    CatalogPage resolvePage(SelectionGroup selection, String cursor);
    record CatalogPage(List<QuestionCatalogEntry> entries, String nextCursor) { }
}
