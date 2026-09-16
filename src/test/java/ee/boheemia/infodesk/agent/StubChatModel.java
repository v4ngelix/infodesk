package ee.boheemia.infodesk.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class StubChatModel implements ChatModel {

	final List<Prompt> prompts = new ArrayList<>();

	private final Deque<String> replies = new ArrayDeque<>();

	StubChatModel reply(String... texts) {
		replies.addAll(List.of(texts));
		return this;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		prompts.add(prompt);
		String text = replies.isEmpty() ? "" : replies.poll();
		return ChatResponse.builder()
				.generations(List.of(new Generation(AssistantMessage.builder().content(text).build())))
				.build();
	}

	@Override
	public ChatOptions getOptions() {
		return ToolCallingChatOptions.builder().build();
	}

}
