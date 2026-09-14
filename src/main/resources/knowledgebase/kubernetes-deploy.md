# Kubernetes deploy protsess

Kirjeldus, kuidas rakendus jõuab Kubernetes klastrisse arendus-, test- ja toodangukeskkonda.

## Ülevaade

Rakendusi ei paigaldata klastrisse käsitsi. Deploy toimub GitOps põhimõttel: keskkonna soovitud olek on kirjas eraldi konfiguratsioonirepos ja klastris töötav sünkroniseerija rakendab muudatused automaatselt.

## Eeldused

- Rakendusel on Helm chart, mis asub rakenduse repos kaustas `deploy/`.
- Konteineri image on ehitatud CI/CD pipeline'is ja salvestatud sisemisse image registrisse.
- Tiimil on oma namespace, mille loomist saab taotleda teenuste portaalis.

## Deploy sammud

1. Pipeline ehitab image ja märgib selle commit'i lühikese räsiga.
2. Arenduskeskkonda (dev) toimub deploy automaatselt pärast edukat pipeline'i `main` harul.
3. Testkeskkonda (test) deploy jaoks ava konfiguratsioonirepos merge request, mis uuendab image versiooni.
4. Toodangusse (prod) deploy nõuab muudatuste taotlust ja kahe inimese kinnitust merge request'is.
5. Sünkroniseerija rakendab muudatuse mõne minuti jooksul; oleku näed klastri halduspaneelilt.

## Tagasipööramine

Kui uus versioon ei tööta, pööra tagasi konfiguratsioonirepo viimane merge request (revert). Sünkroniseerija taastab eelmise versiooni. Käsitsi `kubectl` muudatusi toodangus ei tehta — need kirjutatakse järgmisel sünkroniseerimisel üle.

## Saladused ja seadistus

Saladusi (paroolid, võtmed) ei hoita Helm chart'is ega repos. Need hallatakse keskses saladuste halduses ja seotakse podiga keskkonnamuutujatena.
