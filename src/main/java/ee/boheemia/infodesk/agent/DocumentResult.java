package ee.boheemia.infodesk.agent;

import ee.boheemia.infodesk.knowledgebase.KnowledgeBaseDocument;

public record DocumentResult(boolean found, String file, String title, String body, String message) {

	static final String NOT_FOUND_MESSAGE = "Dokumenti ei leitud";

	static DocumentResult of(KnowledgeBaseDocument document) {
		return new DocumentResult(true, document.file(), document.title(), document.body(), null);
	}

	static DocumentResult notFound() {
		return new DocumentResult(false, null, null, null, NOT_FOUND_MESSAGE);
	}

}
