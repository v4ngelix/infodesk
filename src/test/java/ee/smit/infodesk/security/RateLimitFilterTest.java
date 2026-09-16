package ee.smit.infodesk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import ee.smit.infodesk.agent.ChatAgentService;
import ee.smit.infodesk.api.AgentController;
import ee.smit.infodesk.api.AskResponse;
import ee.smit.infodesk.health.HealthController;

@WebMvcTest(controllers = { AgentController.class, HealthController.class },
		properties = "infodesk.rate-limit.requests-per-minute=10")
@ActiveProfiles("test")
class RateLimitFilterTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ChatAgentService agentService;

	@Test
	void eleventhRequestInAMinuteReturns429() {
		given(agentService.ask(any(), any())).willReturn(new AskResponse("a", List.of(), "low", true, "r", "s"));

		for (int i = 0; i < 10; i++) {
			assertThat(ask("10.0.0.1")).hasStatusOk();
		}
		assertThat(ask("10.0.0.1"))
				.hasStatus(HttpStatus.TOO_MANY_REQUESTS)
				.bodyJson()
				.hasPathSatisfying("$.error", e -> assertThat(e).asString().isNotBlank());

		assertThat(ask("10.0.0.2")).as("other clients are not affected").hasStatusOk();
	}

	@Test
	void healthIsNotRateLimited() {
		for (int i = 0; i < 15; i++) {
			assertThat(mvc.get().uri("/api/v1/health").with(ip("10.0.0.3"))).hasStatusOk();
		}
	}

	private MockMvcTester.MockMvcRequestBuilder ask(String ip) {
		return mvc.post().uri("/api/v1/agent/ask").with(ip(ip))
				.contentType(MediaType.APPLICATION_JSON).content("{\"question\": \"GitLab?\"}");
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

}
