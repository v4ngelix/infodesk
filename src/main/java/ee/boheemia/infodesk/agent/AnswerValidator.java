package ee.boheemia.infodesk.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ee.boheemia.infodesk.api.AskResponse;
import ee.boheemia.infodesk.api.SourceDto;
import ee.boheemia.infodesk.knowledgebase.KnowledgeBaseDocument;
import ee.boheemia.infodesk.knowledgebase.KnowledgeBaseRepository;
import ee.boheemia.infodesk.knowledgebase.SearchHit;

@Component
public class AnswerValidator {

	public static final String CANARY = "INFODESK-SYS-7f3a";

	public static final String NO_SOURCE_REASON = "Vastust ei õnnestunud siduda teadmusbaasi allikaga.";

	static final String LEAK_REASON = "Vastust ei saa kuvada.";

	static final String UNPARSEABLE_REASON = "Mudeli vastust ei õnnestunud töödelda.";

	static final String DEFAULT_REFUSAL_REASON = "Küsimusele ei saa teadmusbaasi põhjal vastata.";

	private static final Set<String> CONFIDENCE_LEVELS = Set.of("high", "medium", "low");

	private static final Logger log = LoggerFactory.getLogger(AnswerValidator.class);

	private final KnowledgeBaseRepository repository;

	public AnswerValidator(KnowledgeBaseRepository repository) {
		this.repository = repository;
	}

	public AskResponse validate(String question, AgentLlmOutput output, String sessionId) {
		if (output == null) {
			return refusal(UNPARSEABLE_REASON, sessionId);
		}
		if (contains(output.answer(), CANARY) || contains(output.refusalReason(), CANARY)) {
			log.warn("model output contained the system prompt canary; refusing");
			return refusal(LEAK_REASON, sessionId);
		}
		if (output.refused()) {
			String reason = output.refusalReason() == null || output.refusalReason().isBlank() ? DEFAULT_REFUSAL_REASON
					: output.refusalReason().strip();
			return refusal(reason, sessionId);
		}
		List<SourceDto> sources = resolveSources(question, output.sourceFiles());
		if (sources.isEmpty() || output.answer() == null || output.answer().isBlank()) {
			log.info("answer without a verifiable source converted to refusal (files={})", output.sourceFiles());
			return refusal(NO_SOURCE_REASON, sessionId);
		}
		String answer = cite(output.answer().strip(), sources);
		return new AskResponse(answer, sources, normalizeConfidence(output.confidence()), false, null, sessionId);
	}

	private List<SourceDto> resolveSources(String question, List<String> files) {
		if (files == null) {
			return List.of();
		}
		List<SearchHit> hits = repository.search(question);
		List<SourceDto> sources = new ArrayList<>();
		for (String file : new LinkedHashSet<>(files)) {
			if (file == null) {
				continue;
			}
			String name = file.strip();
			repository.get(name).ifPresentOrElse(
					doc -> sources.add(new SourceDto(doc.file(), doc.title(), excerpt(doc, hits))),
					() -> log.warn("model cited unknown file; dropped (len={})", name.length()));
		}
		return List.copyOf(sources);
	}

	private String excerpt(KnowledgeBaseDocument doc, List<SearchHit> hits) {
		return hits.stream()
				.filter(hit -> hit.file().equals(doc.file()))
				.map(SearchHit::excerpt)
				.findFirst()
				.or(() -> Optional.of(repository.summary(doc)).filter(summary -> !summary.isBlank()))
				.orElse(doc.title());
	}

	private static String cite(String answer, List<SourceDto> sources) {
		StringBuilder cited = new StringBuilder(answer);
		for (SourceDto source : sources) {
			String citation = "[allikas: " + source.file() + "]";
			if (!answer.contains(citation)) {
				cited.append(' ').append(citation);
			}
		}
		return cited.toString();
	}

	private static String normalizeConfidence(String confidence) {
		if (confidence == null) {
			return "low";
		}
		String normalized = confidence.strip().toLowerCase(Locale.ROOT);
		return CONFIDENCE_LEVELS.contains(normalized) ? normalized : "low";
	}

	private static boolean contains(String text, String token) {
		return text != null && text.contains(token);
	}

	private static AskResponse refusal(String reason, String sessionId) {
		return new AskResponse(null, List.of(), "low", true, reason, sessionId);
	}

}
