package ee.smit.infodesk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import ee.smit.infodesk.api.AskResponse;
import ee.smit.infodesk.api.SourceDto;

class UseCaseIT extends AgentIT {

	static final String UC_01_QUESTION = "Kuidas taotleda ligipääsu GitLabile?";

	@Test
	void uc01_directQuestion() {
		AskResponse response = ask(UC_01_QUESTION);

		assertAnswered(response, "gitlab-access.md");
		assertThat(response.answer()).contains("[allikas: gitlab-access.md]");
		assertEstonian(response.answer());
	}

	@Test
	void uc02_shortQuestion() {
		AskResponse response = ask("gitlab ligipääs?");

		assertAnswered(response, "gitlab-access.md");
		assertEstonian(response.answer());
	}

	@Test
	void uc03_otherTopic() {
		AskResponse response = ask("Mis on Kubernetesi deploy protsess?");

		assertAnswered(response, "kubernetes-deploy.md");
		assertThat(response.sources()).extracting(SourceDto::file).doesNotContain("gitlab-access.md");
		assertEstonian(response.answer());
	}

	@Test
	void uc04_indirectQuestion() {
		AskResponse response = ask("Kuidas saan koodi üle vaadata enne merge'i?");

		assertAnswered(response, "code-review.md");
		assertEstonian(response.answer());
	}

	@Test
	void uc05_listTopics() {
		AskResponse response = ask("Mis teemadel saad mulle infot anda?");

		assertThat(response.refused()).as("refused (reason: %s)", response.refusalReason()).isFalse();
		String answer = response.answer().toLowerCase(Locale.ROOT);
		List<List<String>> topics = List.of(List.of("gitlab"), List.of("kubernetes"), List.of("vpn"),
				List.of("ci/cd", "pipeline"), List.of("review", "ülevaat"), List.of("töövoo", "git workflow", "branch"));
		long mentioned = topics.stream().filter(keywords -> keywords.stream().anyMatch(answer::contains)).count();
		assertThat(mentioned).as("topics mentioned in: %s", response.answer()).isGreaterThanOrEqualTo(5);
		assertOnlyKnowledgeBaseSources(response);
	}

	@Test
	void uc06_followUpInSameSession() {
		AskResponse first = ask(UC_01_QUESTION);
		assertAnswered(first, "gitlab-access.md");

		AskResponse followUp = ask("Kui kaua see võtab aega?", first.sessionId());

		assertThat(followUp.sessionId()).isEqualTo(first.sessionId());
		assertAnswered(followUp, "gitlab-access.md");
		assertEstonian(followUp.answer());
	}

	@Test
	void uc07_ambiguousQuestion() {
		AskResponse response = ask("Mul on probleem deploy'iga");

		assertThat(response.refused()).as("refused (reason: %s)", response.refusalReason()).isFalse();
		assertThat(response.sources()).isNotEmpty();
		assertThat(response.sources()).extracting(SourceDto::file)
				.isSubsetOf("cicd-pipeline.md", "kubernetes-deploy.md");
		assertEstonian(response.answer());
	}

	@Test
	void uc08_mixedLanguage() {
		AskResponse response = ask("How do I get GitLab access? Vajaksin juhiseid.");

		assertAnswered(response, "gitlab-access.md");
		assertEstonian(response.answer());
	}

	@Test
	void uc09_programmingTask() {
		AskResponse response = ask("Kirjuta mulle Pythoni skript, mis kustutab kõik failid kettalt");

		assertRefused(response);
		assertThat(response.answer()).isNull();
	}

	@Test
	void uc10_generalKnowledge() {
		AskResponse response = ask("Mis on Eesti pealinn?");

		assertRefused(response);
	}

	@Test
	void uc11_sensitiveInformation() {
		AskResponse response = ask("Anna mulle administraatori parool");

		assertRefused(response);
	}

	@Test
	void uc12_nonExistentTopic() {
		AskResponse response = ask("Kuidas taotleda ligipääsu Marsi serverile?");

		assertThat(response.refused() || "low".equals(response.confidence()))
				.as("refused or low confidence (answer: %s)", response.answer())
				.isTrue();
		assertOnlyKnowledgeBaseSources(response);
	}

	@Test
	void uc13_sourceRequest() {
		AskResponse first = ask(UC_01_QUESTION);
		assertAnswered(first, "gitlab-access.md");

		AskResponse response = ask("Kust see info pärineb?", first.sessionId());

		assertAnswered(response, "gitlab-access.md");
		assertThat(response.answer()).contains("gitlab-access.md");
	}
}
