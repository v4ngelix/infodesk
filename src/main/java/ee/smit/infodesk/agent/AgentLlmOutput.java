package ee.smit.infodesk.agent;

import java.util.List;

/**
 * What the model is asked to return (see {@code prompts/system.st}). Only file names come from the model;
 * {@link AnswerValidator} verifies them against the repository and builds the public response.
 */
public record AgentLlmOutput(
		String answer,
		List<String> sourceFiles,
		String confidence,
		boolean refused,
		String refusalReason) {
}
