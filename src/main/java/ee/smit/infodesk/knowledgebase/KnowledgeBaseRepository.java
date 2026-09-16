package ee.smit.infodesk.knowledgebase;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseRepository {

	private static final String LOCATION = "classpath:knowledgebase/*.md";

	private static final int MAX_EXCERPT_LENGTH = 400;

	private static final int MIN_TOKEN_LENGTH = 2;

	private static final int MIN_PREFIX_MATCH_LENGTH = 3;

	private static final Set<String> TWO_LETTER_WHITELIST = Set.of("ci", "cd", "mr", "pr", "qa", "ui", "db", "vm", "ip",
			"os", "ad");

	private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

	private final Map<String, KnowledgeBaseDocument> documents;

	public KnowledgeBaseRepository() {
		this.documents = load();
	}

	public List<TopicSummary> listTopics() {
		return documents.values().stream()
				.map(doc -> new TopicSummary(doc.file(), doc.title(), summary(doc)))
				.toList();
	}

	public Optional<KnowledgeBaseDocument> get(String file) {
		return file == null ? Optional.empty() : Optional.ofNullable(documents.get(file));
	}

	public List<SearchHit> search(String query) {
		Set<String> queryTokens = queryTokens(query);
		if (queryTokens.isEmpty()) {
			return List.of();
		}
		record Ranked(SearchHit hit, int documentScore) {
		}
		List<Ranked> ranked = new ArrayList<>();
		for (KnowledgeBaseDocument doc : documents.values()) {
			KnowledgeBaseDocument.Section best = null;
			int bestScore = 0;
			for (KnowledgeBaseDocument.Section section : doc.sections()) {
				int score = countMatches(queryTokens, documentTokens(section.heading() + " " + section.text()));
				if (score > bestScore) {
					best = section;
					bestScore = score;
				}
			}
			if (best != null) {
				Set<String> docTokens = documentTokens(doc.title() + " " + doc.body());
				Map<Boolean, List<String>> terms = queryTokens.stream()
						.sorted()
						.collect(Collectors.partitioningBy(q -> matches(q, docTokens)));
				SearchHit hit = new SearchHit(doc.file(), doc.title(), excerpt(best.text()), bestScore,
						terms.get(true), terms.get(false));
				ranked.add(new Ranked(hit, terms.get(true).size()));
			}
		}
		return ranked.stream()
				.sorted(Comparator.comparingInt((Ranked r) -> r.hit().score()).reversed()
						.thenComparing(Comparator.comparingInt(Ranked::documentScore).reversed())
						.thenComparing(r -> r.hit().file()))
				.map(Ranked::hit)
				.toList();
	}

	private static Map<String, KnowledgeBaseDocument> load() {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
			Map<String, KnowledgeBaseDocument> loaded = new TreeMap<>();
			for (Resource resource : resources) {
				try (InputStream in = resource.getInputStream()) {
					String file = resource.getFilename();
					loaded.put(file, parse(file, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
				}
			}
			if (loaded.isEmpty()) {
				throw new IllegalStateException("No knowledge base documents found at " + LOCATION);
			}
			return Collections.unmodifiableMap(loaded);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load knowledge base", ex);
		}
	}

	static KnowledgeBaseDocument parse(String file, String markdown) {
		List<String> lines = markdown.replace("\r\n", "\n").lines().toList();
		int start = 0;
		while (start < lines.size() && lines.get(start).isBlank()) {
			start++;
		}
		String title = file;
		if (start < lines.size() && lines.get(start).startsWith("# ")) {
			title = lines.get(start).substring(2).strip();
			start++;
		}
		List<String> bodyLines = lines.subList(start, lines.size());

		List<KnowledgeBaseDocument.Section> sections = new ArrayList<>();
		String heading = title;
		StringBuilder text = new StringBuilder();
		for (String line : bodyLines) {
			if (line.startsWith("## ")) {
				addSection(sections, heading, text);
				heading = line.substring(3).strip();
				text.setLength(0);
			}
			else {
				text.append(line).append('\n');
			}
		}
		addSection(sections, heading, text);
		return new KnowledgeBaseDocument(file, title, String.join("\n", bodyLines).strip(), sections);
	}

	private static void addSection(List<KnowledgeBaseDocument.Section> sections, String heading, StringBuilder text) {
		String content = text.toString().strip();
		if (!content.isEmpty()) {
			sections.add(new KnowledgeBaseDocument.Section(heading, content));
		}
	}

	public String summary(KnowledgeBaseDocument doc) {
		return doc.sections().isEmpty() ? "" : excerpt(doc.sections().getFirst().text().split("\n\n")[0]);
	}

	private static Set<String> documentTokens(String text) {
		return Set.copyOf(words(text));
	}

	private static Set<String> queryTokens(String query) {
		return words(query).stream()
				.filter(word -> word.length() >= MIN_PREFIX_MATCH_LENGTH || TWO_LETTER_WHITELIST.contains(word))
				.collect(Collectors.toSet());
	}

	private static List<String> words(String text) {
		if (text == null) {
			return List.of();
		}
		String normalized = DIACRITICS.matcher(Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
				.replaceAll("");
		return Arrays.stream(NON_LETTERS.split(normalized))
				.filter(word -> word.length() >= MIN_TOKEN_LENGTH)
				.toList();
	}

	private static int countMatches(Set<String> queryTokens, Set<String> documentTokens) {
		return (int) queryTokens.stream().filter(q -> matches(q, documentTokens)).count();
	}

	private static boolean matches(String queryToken, Set<String> documentTokens) {
		return queryToken.length() < MIN_PREFIX_MATCH_LENGTH ? documentTokens.contains(queryToken)
				: documentTokens.stream().anyMatch(d -> d.startsWith(queryToken));
	}

	private static String excerpt(String text) {
		String flat = text.replaceAll("\\s+", " ").strip();
		if (flat.length() <= MAX_EXCERPT_LENGTH) {
			return flat;
		}
		String cut = flat.substring(0, MAX_EXCERPT_LENGTH - 1);
		int lastSpace = cut.lastIndexOf(' ');
		return (lastSpace > 0 ? cut.substring(0, lastSpace) : cut) + "…";
	}

}
