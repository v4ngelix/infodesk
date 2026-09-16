package ee.smit.infodesk.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;

import ee.smit.infodesk.api.AskResponse;
import ee.smit.infodesk.security.GuardResult;
import ee.smit.infodesk.security.InputGuard;

@Service
public class ChatAgentService {

	private static final Logger log = LoggerFactory.getLogger(ChatAgentService.class);

	private final InputGuard inputGuard;

	private final AnswerValidator validator;

	private final ChatClient chatClient;

	public ChatAgentService(ChatClient.Builder chatClientBuilder, KnowledgeBaseTools tools, InputGuard inputGuard,
			AnswerValidator validator, @Value("classpath:prompts/system.st") Resource systemPrompt,
			@Value("${infodesk.memory-window:10}") int memoryWindow) {
		this.inputGuard = inputGuard;
		this.validator = validator;
		String prompt = read(systemPrompt);
		if (!prompt.contains(AnswerValidator.CANARY)) {
			throw new IllegalStateException("System prompt must contain the canary token " + AnswerValidator.CANARY);
		}
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(memoryWindow).build();
		this.chatClient = chatClientBuilder
				.defaultSystem(prompt)
				.defaultTools(tools)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.build();
	}

	public AskResponse ask(String question, String sessionId) {
		String session = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;

		Optional<String> invalid = inputGuard.validate(question);
		if (invalid.isPresent()) {
			return refusal(invalid.get(), session);
		}
		GuardResult guard = inputGuard.scan(question);
		if (guard.suspicious()) {
			return refusal(InputGuard.REFUSAL_REASON, session);
		}

		AgentLlmOutput output;
		try {
			output = chatClient.prompt()
					.user(question)
					.advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, session))
					.call()
					.entity(AgentLlmOutput.class);
		}
		catch (IllegalStateException | JacksonException ex) {
			log.warn("model output could not be parsed as {}: {}", AgentLlmOutput.class.getSimpleName(),
					ex.getClass().getName());
			output = null;
		}
		return validator.validate(question, output, session);
	}

	private static AskResponse refusal(String reason, String session) {
		return new AskResponse(null, List.of(), "low", true, reason, session);
	}

	private static String read(Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read system prompt " + resource.getDescription(), ex);
		}
	}

}
