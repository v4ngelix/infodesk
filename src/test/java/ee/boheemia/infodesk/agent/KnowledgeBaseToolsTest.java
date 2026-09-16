package ee.boheemia.infodesk.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ee.boheemia.infodesk.knowledgebase.KnowledgeBaseRepository;

class KnowledgeBaseToolsTest {

	private final KnowledgeBaseTools tools = new KnowledgeBaseTools(new KnowledgeBaseRepository());

	@Test
	void listTopicsReturnsAllDocuments() {
		assertThat(tools.listTopics()).extracting("file").contains("gitlab-access.md", "cicd-pipeline.md");
	}

	@Test
	void searchReturnsAtMostThreeHitsWithFileTitleAndExcerpt() {
		assertThat(tools.searchKnowledgeBase("gitlab ligipääs kubernetes deploy review pipeline"))
				.hasSizeBetween(1, KnowledgeBaseTools.MAX_SEARCH_HITS)
				.allSatisfy(hit -> {
					assertThat(hit.file()).isNotBlank();
					assertThat(hit.title()).isNotBlank();
					assertThat(hit.excerpt()).isNotBlank();
				});
	}

	@Test
	void searchAllowsSlashesInQuery() {
		assertThat(tools.searchKnowledgeBase("  CI/CD pipeline  ")).first()
				.extracting("file").isEqualTo("cicd-pipeline.md");
	}

	@Test
	void searchForPathTraversalReturnsNoFilesystemContent() {
		assertThat(tools.searchKnowledgeBase("../../../etc/passwd"))
				.allSatisfy(hit -> assertThat(hit.excerpt()).doesNotContain("root:"));
	}

	@Test
	void searchCapsQueryLength() {
		KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);

		new KnowledgeBaseTools(repository).searchKnowledgeBase("  " + "a".repeat(500) + "  ");

		verify(repository).search("a".repeat(KnowledgeBaseTools.MAX_INPUT_LENGTH));
	}

	@Test
	void searchHandlesNull() {
		assertThat(tools.searchKnowledgeBase(null)).isEmpty();
	}

	@Test
	void getDocumentReturnsFullDocumentForKnownFile() {
		DocumentResult result = tools.getDocument(" gitlab-access.md ");

		assertThat(result.found()).isTrue();
		assertThat(result.file()).isEqualTo("gitlab-access.md");
		assertThat(result.title()).isEqualTo("GitLab ligipääs");
		assertThat(result.body()).contains("1–2 tööpäeva");
		assertThat(result.message()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "../../../etc/passwd", "/etc/passwd", "knowledgebase/gitlab-access.md",
			"knowledgebase\\gitlab-access.md", "..gitlab-access.md", "C:\\Windows\\win.ini" })
	void getDocumentRejectsPathLikeNamesWithoutTouchingRepository(String file) {
		KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);

		assertNotFound(new KnowledgeBaseTools(repository).getDocument(file));
		verifyNoInteractions(repository);
	}

	@Test
	void getDocumentRejectsOverlongNameWithoutTouchingRepository() {
		KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);

		assertNotFound(new KnowledgeBaseTools(repository).getDocument("a".repeat(201) + ".md"));
		verifyNoInteractions(repository);
	}

	@Test
	void getDocumentReturnsPlainNotFoundForUnknownOrBlankNames() {
		assertNotFound(tools.getDocument("missing.md"));
		assertNotFound(tools.getDocument("   "));
		assertNotFound(tools.getDocument(null));
	}

	private static void assertNotFound(DocumentResult result) {
		assertThat(result).isEqualTo(new DocumentResult(false, null, null, null, "Dokumenti ei leitud"));
	}

}
