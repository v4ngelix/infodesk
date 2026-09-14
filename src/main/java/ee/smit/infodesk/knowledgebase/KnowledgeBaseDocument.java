package ee.smit.infodesk.knowledgebase;

import java.util.List;

/** One Markdown file from the knowledge base. {@code body} is the content below the {@code # Title} line. */
public record KnowledgeBaseDocument(String file, String title, String body, List<Section> sections) {

	public KnowledgeBaseDocument {
		sections = List.copyOf(sections);
	}

	/** Text under one heading; the intro before the first {@code ##} uses the document title as heading. */
	public record Section(String heading, String text) {
	}

}
