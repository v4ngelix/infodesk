package ee.smit.infodesk;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class InfodeskApplicationTests {

	@MockitoBean
	ChatModel chatModel;

	@Test
	void contextLoads() {
	}

}
