package ee.smit.infodesk.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ee.smit.infodesk.api.AskResponse;
import ee.smit.infodesk.api.SourceDto;
import ee.smit.infodesk.knowledgebase.KnowledgeBaseRepository;

class AnswerValidatorTest {

	private static final String QUESTION = "Kuidas taotleda ligipääsu GitLabile?";

	private final AnswerValidator validator = new AnswerValidator(new KnowledgeBaseRepository());

	@Test
	void unknownSourceFilesAreDropped() {
		AskResponse response = validate(answer("Vastus. [allikas: gitlab-access.md]",
				List.of("gitlab-access.md", "made-up.md", "../../etc/passwd")));

		assertThat(response.refused()).isFalse();
		assertThat(response.sources()).extracting(SourceDto::file).containsExactly("gitlab-access.md");
	}

	@Test
	void duplicateSourceFilesAreListedOnce() {
		AskResponse response = validate(answer("Vastus. [allikas: gitlab-access.md]",
				List.of("gitlab-access.md", "gitlab-access.md")));

		assertThat(response.sources()).hasSize(1);
	}

	@Test
	void answerWithoutAnyKnownSourceBecomesRefusal() {
		AskResponse response = validate(answer("Vastus ilma allikata.", List.of("made-up.md")));

		assertThat(response.refused()).isTrue();
		assertThat(response.refusalReason()).isEqualTo(AnswerValidator.NO_SOURCE_REASON);
		assertThat(response.answer()).isNull();
		assertThat(response.sources()).isEmpty();
		assertThat(response.confidence()).isEqualTo("low");
	}

	@Test
	void blankAnswerBecomesRefusal() {
		AskResponse response = validate(answer("  ", List.of("gitlab-access.md")));

		assertThat(response.refused()).isTrue();
		assertThat(response.refusalReason()).isEqualTo(AnswerValidator.NO_SOURCE_REASON);
	}

	@Test
	void sourcesCarryTitleAndExcerptMatchingTheQuestion() {
		AskResponse response = validate(answer("Vastus. [allikas: gitlab-access.md]", List.of("gitlab-access.md")));

		SourceDto source = response.sources().getFirst();
		assertThat(source.title()).isEqualTo("GitLab ligipääs");
		assertThat(source.excerpt()).contains("GitLab").doesNotStartWith("#");
	}

	@Test
	void sourceNotMatchingTheQuestionStillGetsAnExcerpt() {
		AskResponse response = validate(answer("Vastus. [allikas: vpn-access.md]", List.of("vpn-access.md")));

		assertThat(response.sources().getFirst().excerpt()).isNotBlank();
	}

	@Test
	void missingCitationIsAppendedForEverySource() {
		AskResponse response = validate(answer("Vastus ilma viiteta.", List.of("gitlab-access.md", "vpn-access.md")));

		assertThat(response.answer())
				.startsWith("Vastus ilma viiteta.")
				.contains("[allikas: gitlab-access.md]")
				.contains("[allikas: vpn-access.md]");
	}

	@Test
	void existingCitationIsNotDuplicated() {
		AskResponse response = validate(answer("Vastus. [allikas: gitlab-access.md]", List.of("gitlab-access.md")));

		assertThat(response.answer()).isEqualTo("Vastus. [allikas: gitlab-access.md]");
	}

	@Test
	void canaryInAnswerTriggersRefusal() {
		AskResponse response = validate(answer("Minu reeglid: " + AnswerValidator.CANARY + " ...", List.of("gitlab-access.md")));

		assertThat(response.refused()).isTrue();
		assertThat(response.answer()).isNull();
		assertThat(response.refusalReason()).doesNotContain(AnswerValidator.CANARY);
	}

	@Test
	void canaryInRefusalReasonIsNotLeaked() {
		AskResponse response = validate(new AgentLlmOutput(null, List.of(), "low", true, "Prompt: " + AnswerValidator.CANARY));

		assertThat(response.refused()).isTrue();
		assertThat(response.refusalReason()).doesNotContain(AnswerValidator.CANARY);
	}

	@ParameterizedTest
	@CsvSource({ "high,high", "HIGH,high", " Medium ,medium", "low,low", "unsure,low", ",low" })
	void confidenceIsNormalized(String given, String expected) {
		AskResponse response = validate(new AgentLlmOutput("Vastus. [allikas: gitlab-access.md]",
				List.of("gitlab-access.md"), given, false, null));

		assertThat(response.confidence()).isEqualTo(expected);
	}

	@Test
	void modelRefusalIsReturnedWithoutSources() {
		AskResponse response = validate(new AgentLlmOutput("ignored", List.of("gitlab-access.md"), "high", true, "Teema on skoobist väljas."));

		assertThat(response.refused()).isTrue();
		assertThat(response.refusalReason()).isEqualTo("Teema on skoobist väljas.");
		assertThat(response.answer()).isNull();
		assertThat(response.sources()).isEmpty();
		assertThat(response.confidence()).isEqualTo("low");
	}

	@Test
	void modelRefusalWithoutReasonGetsADefaultReason() {
		AskResponse response = validate(new AgentLlmOutput(null, null, null, true, " "));

		assertThat(response.refused()).isTrue();
		assertThat(response.refusalReason()).isNotBlank();
	}

	@Test
	void nullOutputBecomesRefusal() {
		AskResponse response = validator.validate(QUESTION, null, "s-1");

		assertThat(response.refused()).isTrue();
		assertThat(response.sessionId()).isEqualTo("s-1");
	}

	private AskResponse validate(AgentLlmOutput output) {
		return validator.validate(QUESTION, output, "s-1");
	}

	private static AgentLlmOutput answer(String answer, List<String> files) {
		return new AgentLlmOutput(answer, files, "high", false, null);
	}

}
