package ee.smit.infodesk.agent;

import ee.smit.infodesk.api.AskResponse;

public interface AgentService {

	AskResponse ask(String question, String sessionId);

}
