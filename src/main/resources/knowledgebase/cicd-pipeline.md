# CI/CD pipeline

Ülevaade GitLab CI pipeline'i standardsest ülesehitusest ja sellest, mida teha, kui pipeline ebaõnnestub.

## Pipeline'i etapid

1. **build** — koodi kompileerimine ja sõltuvuste laadimine.
2. **test** — unit testid ja koodi kvaliteedi kontroll.
3. **security** — sõltuvuste haavatavuste skaneerimine ja saladuste tuvastamine koodis.
4. **package** — konteineri image ehitamine ja registrisse laadimine.
5. **deploy** — arenduskeskkonda paigaldamine (ainult `main` harul).

## Seadistamine

Pipeline kirjeldatakse projekti juurkaustas failis `.gitlab-ci.yml`. Soovitatav on kasutada ühiseid malle (include), mitte kirjutada etappe nullist. Mallid uuendatakse tsentraalselt ja sisaldavad kohustuslikke turvakontrolle.

## Kui pipeline ebaõnnestub

- Ava ebaõnnestunud job ja loe logi lõpust esimene veateade.
- Testide vea korral käivita samad testid kohalikult enne uue commit'i tegemist.
- Turvaskaneerimise vea korral uuenda haavatav sõltuvus; erandi saab taotleda ainult turvatiimi kinnitusega.
- Kui runner ei vasta üle 30 minuti, teavita platvormitiimi teenuste portaali kaudu.

## Head tavad

- Hoia pipeline kiire: täispipeline peaks lõppema alla 15 minuti.
- Ära lisa saladusi `.gitlab-ci.yml` faili — kasuta kaitstud CI muutujaid.
- Merge request'i ei saa ühendada, kui pipeline pole roheline.
