package ee.smit.infodesk.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ee.smit.infodesk.agent.ChatAgentService;
import jakarta.validation.Valid;

@RestController
public class AgentController {

	private final ChatAgentService agentService;

	public AgentController(ChatAgentService agentService) {
		this.agentService = agentService;
	}

	@PostMapping("/api/v1/agent/ask")
	public AskResponse ask(@Valid @RequestBody AskRequest request) {
		return agentService.ask(request.question(), request.sessionId());
	}

}
