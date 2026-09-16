package ee.boheemia.infodesk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ApiStructureIT extends AgentIT {

	@Test
	@DisplayName("API-04 Vastuse struktuur")
	void api04_responseStructure() {
		String body = client.post()
				.uri(ASK_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"question\": \"" + UseCaseIT.UC_01_QUESTION + "\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		JsonNode json = JsonMapper.builder().build().readTree(body);

		assertThat(json.has("answer")).isTrue();
		assertThat(json.get("answer").isString()).isTrue();
		assertThat(json.get("answer").asString()).contains("[allikas:");

		assertThat(json.get("sources").isArray()).isTrue();
		assertThat(json.get("sources").isEmpty()).isFalse();
		for (JsonNode source : json.get("sources")) {
			assertThat(source.get("file").isString()).isTrue();
			assertThat(source.get("excerpt").isString()).isTrue();
			assertThat(source.get("excerpt").asString()).isNotBlank();
		}

		assertThat(json.get("confidence").asString()).isIn("high", "medium", "low");
		assertThat(json.get("refused").isBoolean()).isTrue();
		assertThat(json.get("refused").asBoolean()).isFalse();
		assertThat(json.has("refusalReason")).isTrue();
		assertThat(json.get("refusalReason").isNull()).isTrue();
	}
}
