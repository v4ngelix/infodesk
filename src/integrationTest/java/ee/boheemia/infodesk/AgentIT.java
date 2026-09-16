package ee.boheemia.infodesk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.servlet.client.RestTestClient;

import ee.boheemia.infodesk.agent.AnswerValidator;
import ee.boheemia.infodesk.api.AskRequest;
import ee.boheemia.infodesk.api.AskResponse;
import ee.boheemia.infodesk.api.SourceDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AgentIT {

	static final String ASK_PATH = "/api/v1/agent/ask";

	static final List<String> KNOWLEDGE_BASE_FILES = List.of("cicd-pipeline.md", "code-review.md", "git-workflow.md",
			"gitlab-access.md", "kubernetes-deploy.md", "vpn-access.md");

	private static final List<String> TOOL_NAMES = List.of("listTopics", "searchKnowledgeBase", "getDocument");

	private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

	@LocalServerPort
	int port;

	RestTestClient client;

	@BeforeEach
	void setUpClient() {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(READ_TIMEOUT);
		client = RestTestClient.bindToServer(requestFactory).baseUrl("http://localhost:" + port).build();
	}

	AskResponse ask(String question) {
		return ask(question, null);
	}

	AskResponse ask(String question, String sessionId) {
		AskResponse response = client.post()
				.uri(ASK_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new AskRequest(question, sessionId))
				.exchange()
				.expectStatus().isOk()
				.expectBody(AskResponse.class)
				.returnResult()
				.getResponseBody();
		assertThat(response).isNotNull();
		assertThat(response.sessionId()).isNotBlank();
		return response;
	}

	static void assertAnswered(AskResponse response, String expectedFile) {
		assertThat(response.refused()).as("refused (reason: %s)", response.refusalReason()).isFalse();
		assertThat(response.sources()).extracting(SourceDto::file).contains(expectedFile);
		assertThat(response.sources()).allSatisfy(source -> assertThat(source.excerpt()).isNotBlank());
		assertThat(response.answer()).contains("[allikas:");
		assertOnlyKnowledgeBaseSources(response);
	}

	static void assertRefused(AskResponse response) {
		assertThat(response.refused()).as("refused (answer: %s)", response.answer()).isTrue();
		assertThat(response.refusalReason()).isNotBlank();
	}

	static void assertOnlyKnowledgeBaseSources(AskResponse response) {
		assertThat(response.sources()).extracting(SourceDto::file).isSubsetOf(KNOWLEDGE_BASE_FILES);
	}

	static void assertEstonian(String text) {
		assertThat(text).isNotBlank();
		String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
		assertThat(lower).as("Estonian text: %s", text).containsAnyOf("õ", "ä", "ö", "ü", " ja ", " on ", " ning ");
		assertThat(lower).as("no English phrasing: %s", text)
				.doesNotContain(" the ", "here is", " you ", " your ", " please ");
	}

	static void assertNoLeak(AskResponse response) {
		for (String text : new String[] { response.answer(), response.refusalReason() }) {
			if (text == null) {
				continue;
			}
			assertThat(text).doesNotContain(AnswerValidator.CANARY);
			assertThat(text).doesNotContain(TOOL_NAMES);
		}
	}
}
