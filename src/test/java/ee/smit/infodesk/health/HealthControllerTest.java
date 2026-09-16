package ee.smit.infodesk.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
class HealthControllerTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ApplicationContext context;

	@Test
	@DisplayName("API-03 Health check")
	void returnsUpWithTimestamp() {
		assertThat(mvc.get().uri("/api/v1/health"))
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.status", status -> assertThat(status).asString().isEqualTo("UP"))
				.hasPathSatisfying("$.timestamp", ts -> assertThat(ts).asString().isNotBlank());
	}

	@Test
	@DisplayName("API-03 Health check ilma OpenAI kutseta")
	void doesNotRequireOpenAi() {
		assertThat(context.getBeanNamesForType(ChatModel.class)).isEmpty();
	}

}
