package ee.smit.infodesk.agent;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import ee.smit.infodesk.knowledgebase.KnowledgeBaseRepository;
import ee.smit.infodesk.knowledgebase.SearchHit;
import ee.smit.infodesk.knowledgebase.TopicSummary;

@Component
public class KnowledgeBaseTools {

	static final int MAX_INPUT_LENGTH = 200;

	static final int MAX_SEARCH_HITS = 3;

	private final KnowledgeBaseRepository repository;

	public KnowledgeBaseTools(KnowledgeBaseRepository repository) {
		this.repository = repository;
	}

	@Tool(description = "Loetleb teadmusbaasi teemad.")
	public List<TopicSummary> listTopics() {
		return repository.listTopics();
	}

	@Tool(description = "Otsib teadmusbaasist märksõnade järgi. Tagastab kuni 3 parimat vastet (fail, pealkiri, väljavõte).")
	public List<SearchHit> searchKnowledgeBase(@ToolParam(description = "Otsingusõnad") String query) {
		if (query == null) {
			return List.of();
		}
		return repository.search(cap(query.strip())).stream().limit(MAX_SEARCH_HITS).toList();
	}

	@Tool(description = "Tagastab teadmusbaasi dokumendi täisteksti failinime järgi (nt listTopics tulemusest).")
	public DocumentResult getDocument(@ToolParam(description = "Failinimi, nt gitlab-access.md") String file) {
		if (file == null) {
			return DocumentResult.notFound();
		}
		String name = file.strip();
		if (name.length() > MAX_INPUT_LENGTH || name.contains("/") || name.contains("\\") || name.contains("..")) {
			return DocumentResult.notFound();
		}
		return repository.get(name).map(DocumentResult::of).orElseGet(DocumentResult::notFound);
	}

	private static String cap(String input) {
		return input.length() > MAX_INPUT_LENGTH ? input.substring(0, MAX_INPUT_LENGTH) : input;
	}

}
