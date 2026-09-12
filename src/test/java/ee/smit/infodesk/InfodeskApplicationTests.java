package ee.smit.infodesk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class InfodeskApplicationTests {

	@MockitoBean
	ChatModel chatModel;

	@Value("${spring.ai.openai.api-key:}")
	String apiKey;

	@Test
	void contextLoads() {
	}

	/** The test profile must shadow any real key from the environment or .env. */
	@Test
	void unitTestsNeverUseARealApiKey() {
		assertThat(apiKey).isEqualTo("test-key-not-used");
	}

}
