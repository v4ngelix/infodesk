package ee.smit.infodesk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for the OpenAI wiring: the API key resolves from the environment or
 * from a local {@code .env}, and the configured model actually answers.
 * Requires a working key — run via {@code ./gradlew integrationTest}.
 */
@SpringBootTest
class OpenAiConnectivityIT {

	@Autowired
	ChatModel chatModel;

	@Value("${spring.ai.openai.api-key:}")
	String apiKey;

	@Value("${spring.ai.openai.chat.options.model:}")
	String model;

	@Test
	void apiKeyResolvesFromEnvironmentOrDotEnv() {
		assertThat(apiKey)
				.as("OPENAI_API_KEY must be provided via the environment or .env, never committed")
				.isNotBlank()
				.isNotEqualTo("sk-...");
		assertThat(model).isNotBlank();
	}

	@Test
	void reachesConfiguredOpenAiModel() {
		String answer = chatModel.call("Vasta tapselt uhe sonaga: pong");

		assertThat(answer).isNotBlank();
		assertThat(answer.toLowerCase()).contains("pong");
	}

}
