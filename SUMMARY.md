# Infodesk – kokkuvõte

## 1. Arhitektuur
Rakendus on Spring Boot 4 + Spring AI (OpenAI) teenus, Java 21. Päring liigub läbi kolme kihi, millest igaühel on üks selge vastutus:

1. **API kiht** (`api`, `security`) – `AgentController` võtab vastu `POST /api/v1/agent/ask` päringu. Bean Validation lükkab tühja või üle 2000 märgi pika küsimuse tagasi (HTTP 400) enne, kui äriloogika üldse käivitub. `RateLimitFilter` piirab päringuid IP kohta (vaikimisi 10/min) ja `ApiExceptionHandler` tõlgib vead ühtseks JSON-veaks (400 / 502).
2. **Agendi kiht** (`agent`) – `ChatAgentService` orkestreerib voogu: `InputGuard` → `ChatClient` kutse → `AnswerValidator`. Süsteemiprompt on eraldi failis `prompts/system.st` ja laetakse ChatClienti *system* rolli; kasutaja küsimus läheb alati ainult *user* rolli. Vestlusmälu (`MessageWindowChatMemory`, 10 sõnumit) seob järelküsimused `sessionId` kaudu. Mudel peab vastama struktureeritud JSON-iga (`AgentLlmOutput`), mille rakendus enne kasutajale tagastamist kontrollib.
3. **Teadmusbaasi kiht** (`knowledgebase`) – `KnowledgeBaseRepository` laeb käivitamisel kõik `resources/knowledgebase/*.md` failid mällu ja pakub lihtsat märksõnaotsingut (diakriitikuta, prefiksisobitusega, sektsioonide kaupa). Agent pääseb sellele ligi ainult kolme tööriista kaudu (`listTopics`, `searchKnowledgeBase`, `getDocument`), mis on registreeritud `@Tool` annotatsiooniga ja moodustavad kogu lubatud tööriistade nimekirja.

Kihid on üksteisest sõltumatud ja eraldi testitavad OpenAI-ta (LLM asendatakse testis `StubChatModel`-iga).
Allikaviited ja keeldumised ei sõltu ainult promptist, vaid neid jõustab `AnswerValidator` rakenduse tasemel.

## 2. Turvamehhanismid ja põhjendused
Kaitse on ehitatud kolmes kihis, et ükski üksik kiht (eriti LLM ise) ei oleks ainus barjäär:

- API võtme kuritarvituste eest kaitseb osaliselt `RateLimitFilter` (10 päringut/min IP kohta).
- API võti pole hard-coded ja repos nähtav - seda loeb rakendus keskkonnamuutujast.

## 3. Teadaolevad piirangud
- **Märksõnaotsing** – sünonüüme ja parafraase ei mõisteta. Ebatavalise sõnastusega küsimus võib jääda vastuseta.
- **Mustripõhine InputGuard** – tuvastab mitmesugused injectioniga seotud fraase, aga mitte loomingulisi ümbersõnastusi.
- **Allikakontroll on failitasemel** – rakendus kontrollib vaid, et viidatud fail on olemas ja mitte LLMi vastuse vastavust faili sisule.
- **Mudeli mittedeterminism** – integratsioonitestid kontrollivad käitumist (keeldumine, allikad), mitte täpset teksti.
Vastused võivad käivitusest käivitusse erineda.
