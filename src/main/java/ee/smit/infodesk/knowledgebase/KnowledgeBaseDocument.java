package ee.smit.infodesk.knowledgebase;

import java.util.List;

public record KnowledgeBaseDocument(String file, String title, String body, List<Section> sections) {

	public KnowledgeBaseDocument {
		sections = List.copyOf(sections);
	}

	public record Section(String heading, String text) {
	}

}
