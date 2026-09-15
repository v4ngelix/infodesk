package ee.smit.infodesk.agent;

import java.util.List;

public record AgentLlmOutput(
		String answer,
		List<String> sourceFiles,
		String confidence,
		boolean refused,
		String refusalReason) {
}
