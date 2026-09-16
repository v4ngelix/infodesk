# Infodesk – kokkuvõte

## 1. Arhitektuur
Rakendus on Spring Boot 4 + Spring AI (OpenAI) teenus, Java 21.
Päring liigub läbi kolme kihi, millest igaühel on erinev vastutus:

1. **API kiht** (`api`, `security`) –
`AgentController` võtab vastu `POST /api/v1/agent/ask` päringu. Bean Validation lükkab tühja või üle 2000 märgi pika küsimuse tagasi (HTTP 400) enne, kui äriloogika üldse käivitub. `RateLimitFilter` piirab päringuid IP kohta (vaikimisi 10/min) ja `ApiExceptionHandler` tõlgib vead ühtseks JSON-veaks (400 / 502).
2. **Agendi kiht** (`agent`) – 
`ChatAgentService` orkestreerib voogu: `InputGuard` → `ChatClient` kutse → `AnswerValidator`. Süsteemiprompt on eraldi failis `prompts/system.st` ja laetakse ChatClienti *system* rolli.
Kasutaja küsimus töödeldaks alati ainult *user* rollis. Vestlusmälu (`MessageWindowChatMemory`, 10 sõnumit) seob järelküsimused `sessionId` kaudu.
Mudel peab vastama struktureeritud JSON-iga (`AgentLlmOutput`), mille rakendus enne kasutajale tagastamist kontrollib.
3. **Teadmusbaasi kiht** (`knowledgebase`) – 
`KnowledgeBaseRepository` laeb käivitamisel kõik `resources/knowledgebase/*.md` failid mällu ja pakub lihtsat märksõnaotsingut.
Agent pääseb sellele ligi ainult kolme tööriista kaudu: `listTopics`, `searchKnowledgeBase`, `getDocument`).

Kihid on üksteisest sõltumatud ja eraldi testitavad OpenAI-ta (LLM asendatakse testis `StubChatModel`-iga).
Allikaviited ja keeldumised ei sõltu ainult promptist, vaid neid jõustab `AnswerValidator` rakenduse tasemel.

## 2. Turvamehhanismid ja põhjendused
Päringutel on kolmekihiline kaitse. Süsteemiprompt üksi ei pruugi kõiki rünnakuid ära kaitsta. 'Kolm etappi kus teostatakse turvatoiminguid:
1. **Enne LLM-i päringut** –
`InputGuard` normaliseerib sisendi (väiketähed, diakriitikud eemaldatud) ja võrdleb seda ~20 mustriga eesti ja inglise keeles.
Sobivuse korral tagastatakse kohe `refused: true` ilma OpenAI kutseta. Logisse läheb ainult mustri ID ja sisendi pikkus, mitte küsimus ise.
**Põhjendus:** kiire, deterministlik ja odav.
2. **Promptis ja tööriistades** –
mittevajalikud OpenAI mudelid (embedding, audio, image, moderation) on konfiguratsioonis välja lülitatud.
Süsteemiprompt on *system* rollis, kasutaja tekst ainult *user* rollis.
Süsteemiprompt määrab selgelt, et kasutaja sisendit ei või käsitleda juhistena ning keelab kõrvalise info avaldamise.
Tööriistu on täpselt kolm ja need loevad ainult classpathi mälus olevaid faile: 
   - `getDocument` lükkab tagasi failinimed, mis sisaldavad pathiga seotud märke: `/`, `\`, `..`.
   - `searchKnowledgeBase` lõikab sisendi 200 märgile. Mudelil pole ühtegi "üldist" tööriista, HTTP-, faili- ega käsuligipääsu.
3. **Pärast LLM-i vastust** –
`AnswerValidator` on viimane kaitseliin. Süsteemiprompt sisaldab *canary* märgist `INFODESK-SYS-7f3a`.
Selle ilmumisel vastusesse, on süsteemiprompt lekkinud ja vastus asendatakse keeldumisega.
Kui mudel ütleb `refused: false`, aga ei nimeta ühtki teadmusbaasis olemasolevat faili, muudetakse vastus keeldumiseks kuna `sources` ei tohi kunagi olla tühi.
`excerpt` ei tule mudelilt, vaid rakenduse enda otsingust – nii väldime seda, et mudel mõtleb tsitaate välja.
**Põhjendus:** mudel võib eksida või olla mõjutatud.

- API võtme kuritarvituste eest kaitseb osaliselt `RateLimitFilter` (10 päringut/min IP kohta).
- API võti pole hard-coded ja repos nähtav - seda loeb rakendus keskkonnamuutujast.

## 3. Teadaolevad piirangud
- **Märksõnaotsing** – sünonüüme ja parafraase ei mõisteta. Ebatavalise sõnastusega küsimus võib jääda vastuseta.
- **Mustripõhine InputGuard** – tuvastab mitmesugused injectioniga seotud fraase, aga mitte loomingulisi ümbersõnastusi.
- **Allikakontroll on failitasemel** – rakendus kontrollib vaid, et viidatud fail on olemas ja mitte LLMi vastuse vastavust faili sisule.
- **Mudeli mittedeterminism** – integratsioonitestid kontrollivad käitumist (keeldumine, allikad), mitte täpset teksti.
Vastused võivad käivitusest käivitusse erineda.
