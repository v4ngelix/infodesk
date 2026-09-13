package ee.smit.infodesk.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ee.smit.infodesk.agent.AgentService;
import jakarta.validation.Valid;

@RestController
public class AgentController {

	private final AgentService agentService;

	public AgentController(AgentService agentService) {
		this.agentService = agentService;
	}

	@PostMapping("/api/v1/agent/ask")
	public AskResponse ask(@Valid @RequestBody AskRequest request) {
		return agentService.ask(request.question(), request.sessionId());
	}

}
