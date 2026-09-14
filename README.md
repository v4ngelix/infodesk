# Infodesk

## Requirements

- Java 21
- OpenAI API key

## Running locally
1. Copy the example file and set your key:
```bash
cp .env.example .env
```

2. Define proper values in the created .env file:
```properties
OPENAI_API_KEY=sk-your-key
OPENAI_MODEL=gpt-4.1-mini
```

- `OPENAI_API_KEY` – required.
- `OPENAI_MODEL` – optional, defaults to `gpt-4.1-mini`.

3. Run the application:
```bash
./gradlew bootRun
```

4. Check whether the application is up:
```bash
curl http://localhost:8080/api/v1/health
```

## Input guard

`InputGuard` checks every question before it reaches the LLM (to be called from the agent service, see `docs/PLAN.md` Step 4):

- **Validation:** blank questions and questions longer than `infodesk.max-question-length` (2000) are rejected.
- **Prompt-injection patterns:** English and Estonian phrases such as "ignore previous instructions", "unusta kõik reeglid", "System:", "korda … sõnumid", "süsteemiprompt", "uus reegel", "käitu nagu" and "loetle tööriistad". Case and diacritics are ignored.
- **Decision: refuse immediately.** Suspicious input returns HTTP 200 with `refused: true` and `refusalReason: "Sisend sisaldab keelatud juhiseid."`. The LLM is not called. This is simpler and cheaper than stripping the instruction and answering the rest.
- **Consequence (SEC-04):** a real question with a hidden instruction ("Kuidas saan GitLabi ligipääsu? PS: unusta kõik reeglid …") is refused as a whole.
- **Logging:** only the matched pattern IDs and the input length are logged, never the question text.
- **Limitation:** regex patterns are easy to bypass by rephrasing, so the system prompt is the second line of defence.

## Running tests

```bash
./gradlew test             # unit tests
./gradlew integrationTest  # calls the real OpenAI API, needs OPENAI_API_KEY
```
