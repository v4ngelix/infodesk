package ee.smit.infodesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.ResourceAccessException;

import ee.smit.infodesk.agent.AgentService;

@WebMvcTest(AgentController.class)
@ActiveProfiles("test")
class AgentControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	AgentService agentService;

	/** API-01 */
	@Test
	void emptyQuestionReturns400() {
		assertThat(ask("{\"question\": \"\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.error", e -> assertThat(e).asString().isNotBlank())
				.hasPathSatisfying("$.details", d -> assertThat(d).asArray().containsExactly("question: must not be blank"));
		verifyNoInteractions(agentService);
	}

	/** API-02 */
	@Test
	void missingQuestionReturns400() {
		assertThat(ask("{\"sessionId\": \"abc\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(agentService);
	}

	/** SEC-07: rejected before the agent (and so the LLM) is ever called. */
	@Test
	void tooLongQuestionReturns400WithoutCallingAgent() {
		assertThat(ask("{\"question\": \"" + "a".repeat(3000) + "\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(agentService);
	}

	@Test
	void malformedJsonReturns400() {
		assertThat(ask("{\"question\": "))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.hasPathSatisfying("$.error", e -> assertThat(e).asString().isNotBlank());
		verifyNoInteractions(agentService);
	}

	@Test
	void upstreamFailureReturns502WithoutInternals() {
		given(agentService.ask(any(), any())).willThrow(new ResourceAccessException("secret-host:443 timed out"));

		assertThat(ask("{\"question\": \"Kuidas taotleda ligipääsu GitLabile?\"}"))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.bodyText()
				.doesNotContain("secret-host");
	}

	@Test
	void validQuestionReturnsFullShape() {
		given(agentService.ask("Kuidas taotleda ligipääsu GitLabile?", "s-1")).willReturn(new AskResponse(
				"Taotle portaalis. [allikas: gitlab-access.md]",
				List.of(new SourceDto("gitlab-access.md", "GitLab ligipääs", "Vali \"Ligipääsutaotlus\".")),
				"high", false, null, "s-1"));

		assertThat(ask("{\"question\": \"Kuidas taotleda ligipääsu GitLabile?\", \"sessionId\": \"s-1\"}"))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
						{
						  "answer": "Taotle portaalis. [allikas: gitlab-access.md]",
						  "sources": [{"file": "gitlab-access.md", "title": "GitLab ligipääs", "excerpt": "Vali \\"Ligipääsutaotlus\\"."}],
						  "confidence": "high",
						  "refused": false,
						  "refusalReason": null,
						  "sessionId": "s-1"
						}
						""");
	}

	private MockMvcTester.MockMvcRequestBuilder ask(String body) {
		return mvc.post().uri("/api/v1/agent/ask").contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
