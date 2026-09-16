package ee.smit.infodesk.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class KnowledgeBaseRepositoryTest {

	private final KnowledgeBaseRepository repository = new KnowledgeBaseRepository();

	@Test
	void loadsAtLeastFiveDocumentsWithTitleAndSummary() {
		List<TopicSummary> topics = repository.listTopics();

		assertThat(topics).hasSizeGreaterThanOrEqualTo(5);
		assertThat(topics).extracting(TopicSummary::file).contains("gitlab-access.md", "kubernetes-deploy.md",
				"cicd-pipeline.md", "code-review.md", "git-workflow.md");
		assertThat(topics).allSatisfy(topic -> {
			assertThat(topic.title()).isNotBlank().doesNotStartWith("#");
			assertThat(topic.summary()).isNotBlank();
		});
	}

	@Test
	void getReturnsParsedDocumentWithSections() {
		KnowledgeBaseDocument document = repository.get("gitlab-access.md").orElseThrow();

		assertThat(document.title()).isEqualTo("GitLab ligipääs");
		assertThat(document.body()).contains("1–2 tööpäeva");
		assertThat(document.sections()).isNotEmpty()
				.allSatisfy(section -> assertThat(section.heading()).isNotBlank());
	}

	@Test
	void searchGitlabAccessRanksGitlabDocumentFirst() {
		List<SearchHit> hits = repository.search("gitlab ligipääs");

		assertThat(hits).isNotEmpty();
		assertThat(hits.getFirst().file()).isEqualTo("gitlab-access.md");
	}

	@Test
	void searchReportsMatchedAndUnmatchedTerms() {
		SearchHit hit = repository.search("marsi serveri ligipääs").stream()
				.filter(h -> h.file().equals("gitlab-access.md"))
				.findFirst()
				.orElseThrow();

		assertThat(hit.matchedTerms()).containsExactly("ligipaas");
		assertThat(hit.unmatchedTerms()).containsExactly("marsi", "serveri");
	}

	@Test
	void searchIsCaseAndDiacriticInsensitive() {
		assertThat(repository.search("GITLAB LIGIPAAS")).first()
				.extracting(SearchHit::file).isEqualTo("gitlab-access.md");
	}

	@Test
	void searchKubernetesDeployDoesNotRankGitlabFirst() {
		List<SearchHit> hits = repository.search("kubernetes deploy");

		assertThat(hits).isNotEmpty();
		assertThat(hits.getFirst().file()).isEqualTo("kubernetes-deploy.md");
	}

	@Test
	void searchResultsAreSortedByScoreDescending() {
		List<SearchHit> hits = repository.search("kubernetes deploy pipeline");

		assertThat(hits).extracting(SearchHit::score).isSortedAccordingTo((a, b) -> Integer.compare(b, a));
	}

	@Test
	void excerptIsNonEmptyAndShort() {
		assertThat(repository.search("gitlab ligipääs kubernetes deploy review")).isNotEmpty()
				.allSatisfy(hit -> assertThat(hit.excerpt()).isNotBlank().hasSizeLessThanOrEqualTo(400));
	}

	@Test
	void searchWithoutUsableTokensReturnsNothing() {
		assertThat(repository.search("")).isEmpty();
		assertThat(repository.search("a b ?")).isEmpty();
		assertThat(repository.search(null)).isEmpty();
		assertThat(repository.search("marsi server")).isEmpty();
	}

	@Test
	void searchFindsTwoLetterAcronyms() {
		assertThat(repository.search("CI/CD")).first().extracting(SearchHit::file).isEqualTo("cicd-pipeline.md");
		assertThat(repository.search("ci/cd")).first().extracting(SearchHit::file).isEqualTo("cicd-pipeline.md");
	}

	@Test
	void searchIgnoresTwoLetterWordsNotOnWhitelistInAnyCase() {
		assertThat(repository.search("ja on ei")).isEmpty();
		assertThat(repository.search("JA ON EI")).isEmpty();
		assertThat(repository.search("KU ku")).isEmpty();
		assertThat(repository.search("ja kubernetes ON")).isEqualTo(repository.search("kubernetes"));
	}

	@Test
	void getRejectsPathTraversalAndUnknownNames() {
		assertThat(repository.get("../../../etc/passwd")).isEmpty();
		assertThat(repository.get("/etc/passwd")).isEmpty();
		assertThat(repository.get("knowledgebase/gitlab-access.md")).isEmpty();
		assertThat(repository.get("GITLAB-ACCESS.md")).isEmpty();
		assertThat(repository.get("missing.md")).isEmpty();
		assertThat(repository.get(null)).isEmpty();
	}

	@Test
	void searchForPathTraversalReturnsNoFilesystemContent() {
		assertThat(repository.search("../../../etc/passwd"))
				.allSatisfy(hit -> assertThat(hit.excerpt()).doesNotContain("root:"));
	}

}
