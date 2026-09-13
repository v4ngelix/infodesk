package ee.smit.infodesk.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Max length mirrors {@code infodesk.max-question-length}; annotations need a constant. */
public record AskRequest(
		@NotBlank @Size(max = 2000) String question,
		String sessionId) {
}
