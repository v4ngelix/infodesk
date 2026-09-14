package ee.smit.infodesk.security;

import java.util.List;

/** Result of {@link InputGuard#scan}. {@code matchedPatterns} holds pattern IDs, never input text. */
public record GuardResult(boolean suspicious, List<String> matchedPatterns) {
}
