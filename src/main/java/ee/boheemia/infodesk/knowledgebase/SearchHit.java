package ee.boheemia.infodesk.knowledgebase;

import java.util.List;

public record SearchHit(String file, String title, String excerpt, int score, List<String> matchedTerms,
		List<String> unmatchedTerms) {

	public SearchHit {
		matchedTerms = List.copyOf(matchedTerms);
		unmatchedTerms = List.copyOf(unmatchedTerms);
	}

}
