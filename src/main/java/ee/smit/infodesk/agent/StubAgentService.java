package ee.smit.infodesk.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ee.smit.infodesk.api.AskResponse;

/** Placeholder until the Spring AI agent exists (PLAN.md Step 4). Never calls the LLM. */
@Service
class StubAgentService implements AgentService {

	@Override
	public AskResponse ask(String question, String sessionId) {
		String session = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
		return new AskResponse(null, List.of(), "low", true, "Agent ei ole veel kasutusel.", session);
	}

}
