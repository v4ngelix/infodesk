# Koodireview

Kuidas koodi üle vaadata enne merge'i ja mida review käigus kontrollida.

## Millal review on kohustuslik

Kõik muudatused jõuavad `main` harusse ainult merge request'i (MR) kaudu. Iga MR vajab vähemalt ühe teise arendaja kinnitust. Toodangut mõjutavad ja turvatundlikud muudatused vajavad kahte kinnitust.

## Kuidas review'd küsida

1. Ava GitLabis merge request oma harust `main` harusse.
2. Kirjuta kirjeldusse, mida ja miks muudeti, ning lisa viide tööülesandele.
3. Määra reviewer'iks tiimikaaslane; koodi omanikud (CODEOWNERS) lisatakse automaatselt.
4. Veendu, et pipeline on roheline, enne kui review'd ootad.

## Mida reviewer kontrollib

- Kas muudatus lahendab kirjeldatud ülesande ega sisalda kõrvalist muudatust.
- Kas uuele loogikale on testid.
- Kas koodis pole saladusi, isikuandmeid ega logitavat tundlikku infot.
- Kas nimetamine ja struktuur järgivad projekti tavasid.

## Tagasiside andmine

Kommentaarid peavad olema konkreetsed ja viisakad. Märgi eristatult kohustuslik parandus ja soovitus (nt eesliide "nit:"). Autor lahendab iga arutelu ja reviewer kinnitab MR-i. Review'le tuleks vastata ühe tööpäeva jooksul.

## Merge

Pärast kinnitust ja rohelist pipeline'i ühendab MR-i autor ise. Kasuta "squash" valikut, kui harus on palju väikseid commit'e.
