package ee.smit.infodesk.security;

import java.util.List;

public record GuardResult(boolean suspicious, List<String> matchedPatterns) {
}
