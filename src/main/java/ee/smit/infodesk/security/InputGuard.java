package ee.smit.infodesk.security;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks user input before it reaches the LLM. Suspicious input is refused as a whole, without an LLM call
 * (see README "Input guard"). Only pattern IDs and input length are logged, never the input itself.
 */
@Component
public class InputGuard {

	public static final String REFUSAL_REASON = "Sisend sisaldab keelatud juhiseid.";

	private static final Logger log = LoggerFactory.getLogger(InputGuard.class);

	/** Written lower-case without diacritics; a literal space matches any whitespace run (so write optional spaces as {@code \\s*}). */
	private static final List<InjectionPattern> PATTERNS = List.of(
			InjectionPattern.of("ignore-instructions-en", "ignore (all )?(previous|prior|above) instructions"),
			InjectionPattern.of("forget-rules-en", "forget (your|all|the) rules"),
			InjectionPattern.of("role-override-en", "\\byou are now\\b"),
			InjectionPattern.of("act-as-en", "\\bact as\\b"),
			InjectionPattern.of("system-role", "^\\s*system\\s*:"),
			InjectionPattern.of("system-prompt", "\\bsystem prompt\\b"),
			InjectionPattern.of("forget-rules-et", "unusta (oma|koik) reegl"),
			InjectionPattern.of("ignore-instructions-et", "ignoreeri .*juhise"),
			InjectionPattern.of("role-override-et", "sa oled nuud"),
			InjectionPattern.of("list-tools", "list (all )?(available )?tools"),
			InjectionPattern.of("repeat-messages-en", "repeat .*messages"),
			InjectionPattern.of("repeat-messages-et", "korda .*sonumid"),
			InjectionPattern.of("system-prompt-et", "susteemi\\s*prompt"),
			InjectionPattern.of("new-rule", "\\b(uus|uued) reeg(e)?l|\\bnew rules?\\b"),
			InjectionPattern.of("role-override-not-et", "\\bsa ei ole enam\\b"),
			InjectionPattern.of("act-as-et", "\\bkaitu nagu\\b"),
			InjectionPattern.of("list-tools-et", "\\b(loetle|naita|avalda) .*tooriist"),
			InjectionPattern.of("reveal-rules-et", "\\b(loetle|naita|avalda|utle) .*oma (reegl|juhis)"));

	private final int maxQuestionLength;

	public InputGuard(@Value("${infodesk.max-question-length:2000}") int maxQuestionLength) {
		this.maxQuestionLength = maxQuestionLength;
	}

	/** @return rejection reason, empty when the question is acceptable */
	public Optional<String> validate(String question) {
		if (question == null || question.isBlank()) {
			return Optional.of("Küsimus puudub.");
		}
		if (question.length() > maxQuestionLength) {
			return Optional.of("Küsimus on liiga pikk (lubatud kuni " + maxQuestionLength + " märki).");
		}
		return Optional.empty();
	}

	public GuardResult scan(String question) {
		if (question == null) {
			return new GuardResult(false, List.of());
		}
		String normalized = normalize(question);
		List<String> matched = PATTERNS.stream()
				.filter(pattern -> pattern.regex().matcher(normalized).find())
				.map(InjectionPattern::id)
				.toList();
		if (!matched.isEmpty()) {
			log.warn("suspicious input patterns={} len={}", matched, question.length());
		}
		return new GuardResult(!matched.isEmpty(), matched);
	}

	private static String normalize(String input) {
		return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	private record InjectionPattern(String id, Pattern regex) {

		static InjectionPattern of(String id, String regex) {
			return new InjectionPattern(id, Pattern.compile(regex.replace(" ", "\\s+"), Pattern.MULTILINE | Pattern.DOTALL));
		}

	}

}
