# Infodesk
Käesolev rakendus on lihtne KKK juturobot,
mis suudab lugeda `src/main/resources/knowledgebase` kaustas olevaid "ettevõtte sisemisi tööjuhendeid"
ja neist kasutajale olulisi osi ülesse leida ja edastada.

## Rakenduse käivitamise eeldused
- Java 21
- OpenAI API võti

## Lokaalne arenduskeskkond
Rakenduse jooksutamiseks on vajalik määrata kaks keskkonna muutujat. 
Neid saab seadista IDE kaudu,
kuid kõige lihtsam on projekti lisada eraldi .env fail.

1. Selleks tee juurkaustas olevast .env.example failist endale koopia nimega `.env`:
```bash
cp .env.example .env
```

2. Määra keskkonna muutujad loodud .env failis:
```properties
# Nõutud. Ilma selleta rakendus ei saa toimida.
OPENAI_API_KEY=sk-sinu-võti
# Valikuline. Vaikeväärtus gpt-4.1-mini.
OPENAI_MODEL=gpt-4.1-mini
```

3. Käivita rakendus projekti juurkaustas:
```bash
./gradlew bootRun
```

4. Kontrolli rakenduse töötamist:
```bash
curl http://localhost:8080/api/v1/health
```

5. Küsimuse esitamiseks:
```bash
curl --location 'http://localhost:8080/api/v1/agent/ask' \
--header 'Content-Type: application/json' \
--data '{
    "question": "Mida ma siit küsida saan?",
    "sessionId": "minu-sessioon-1"
}'
```

## Testid
Rakendusel on kahte tüüpi teste, mida saab lokaalselt käivitada järgnevalt:
```bash
./gradlew test             # Üksuste testid - Toimivad ka OpenAI võtmeta.
./gradlew integrationTest  # Integratsiooni testid - Eeldab OpenAI võtit.
```
Testide raportid tekivad lokaalselt kausta `build/reports/tests/`:
- `build/reports/tests/test/index.html` – üksuste testid
- `build/reports/tests/integrationTest/index.html` – integratsiooni testid

## CI
Lähtekoodihoidlal on olemas Github Actions workflow fail `.github/workflows/tests.yml`.
See käivitab testid iga push'i peale.

1. Lisa repositooriumi seadetes OpenAI API võti saladusena (`Settings → Secrets and variables → Actions`):
  ```properties                                                                                                                                                                                                                                                                                                    
  # Nõutud integratsiooni testide jaoks. Ilma selleta integratsiooni testid jäetakse vahele.                                                                                                                                                                                                                       
  OPENAI_API_KEY=sk-sinu-võti                                                                                                                                                                                                                                                                                      
  ```                                                                                                                                                                                                                                                                                                              

2. Tee push suvalisse harusse. Töövoog käivitab mõlemad testi tööd:
  ```bash                                                                                                                                                                                                                                                                                                          
  ./gradlew build            # Kompileerimine ja üksuste testid                                                                                                                                                                                                                                                    
  ./gradlew integrationTest  # Integratsiooni testid (käivitub pärast eelmise töö õnnestumist)                                                                                                                                                                                                                     
  ```                                                                                                                                                                                                                                                                                                              

3. Vaata tulemusi GitHubis `Actions` vahelehel. HTML raportid on allalaaditavad töö artefaktidena:
- `unit-test-report`
- `integration-test-report`

## CD
Hetkel ei ole projektile ülesse seatud automaatset paigutamist.
