package ee.smit.infodesk.api;

import java.util.List;

/** {@code confidence} is one of {@code high}, {@code medium}, {@code low}. */
public record AskResponse(
		String answer,
		List<SourceDto> sources,
		String confidence,
		boolean refused,
		String refusalReason,
		String sessionId) {
}
