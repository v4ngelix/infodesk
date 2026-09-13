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

## Running tests

```bash
./gradlew test             # unit tests
./gradlew integrationTest  # calls the real OpenAI API, needs OPENAI_API_KEY
```
