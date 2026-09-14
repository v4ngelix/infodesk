# Git töövoog

Harude, commit'ide ja versioonide kokkulepped, mida kõik tiimid järgivad.

## Harude mudel

Kasutame trunk-based töövoogu. Põhiharu on `main` ja see peab olema alati paigaldatav. Iga muudatus tehakse lühiajalises harus, mis ühendatakse tagasi mõne päeva jooksul.

## Harude nimetamine

- `feature/<ülesande-nr>-lühikirjeldus` — uus funktsionaalsus
- `fix/<ülesande-nr>-lühikirjeldus` — vea parandus
- `chore/lühikirjeldus` — hooldus, sõltuvuste uuendus

## Commit'i sõnumid

Sõnum algab käskivas kõneviisis lühikese pealkirjaga (kuni 72 märki), nt "Add health endpoint". Vajadusel lisa tühja rea järel selgitus, miks muudatus tehti. Ühes commit'is üks loogiline muudatus.

## Sünkroonis hoidmine

Enne merge request'i avamist uuenda oma haru `main` haru viimase seisuga (`git rebase main` või `git merge main`). Jagatud harudes ära kasuta `git push --force`; vajadusel kasuta `--force-with-lease` ainult oma isiklikus harus.

## Versioonid ja märgendid

Väljalasked märgistatakse semantilise versiooniga, nt `v1.4.0`. Märgendi loob pipeline automaatselt pärast toodangusse deploy kinnitamist.

## Mida mitte commit'ida

Saladused, `.env` failid, build'i väljund ja IDE seadistused kuuluvad `.gitignore` faili. Kui saladus satub kogemata reposse, teavita kohe turvatiimi — ainult commit'i kustutamisest ei piisa.
