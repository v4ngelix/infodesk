package ee.smit.infodesk.api;

import java.util.List;

public record AskResponse(
		String answer,
		List<SourceDto> sources,
		String confidence,
		boolean refused,
		String refusalReason,
		String sessionId) {
}
