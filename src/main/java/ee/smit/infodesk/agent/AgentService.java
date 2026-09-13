package ee.smit.infodesk.agent;

import ee.smit.infodesk.api.AskResponse;

public interface AgentService {

	/**
	 * @param sessionId optional; when blank a new one is generated and returned in the response
	 */
	AskResponse ask(String question, String sessionId);

}
