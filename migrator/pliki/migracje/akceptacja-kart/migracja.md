# Migracja danych: drzewo grup → model Party (przepięte miejsca M1–M4)

## Zakres

### Fakty nowego modelu × przepięte miejsca
| Miejsce | Pytanie (CO) | Fakty, które nowy model musi znać | API nowego modelu (te same polecenia co `PartyAcceptanceWorld`) |
|---|---|---|---|
| M1 | czy karta gościa działa na słupku | słupek → kemping; kemping → region (REGION_MEMBERSHIP); karta (CARD_UID) → posiadacz → wydawca (CARD_ISSUANCE); wydawca → organizacja (ISSUER_MEMBERSHIP); umowy wydawcy (ACCEPTANCE_AGREEMENT → kemping / region); karty bez posiadacza | `pedestals().standsAt`, `parties().registerOrganizationUnit(…, ACCEPTOR/REGION)`, `relationships().assign(…, REGION_MEMBERSHIP / ISSUER_MEMBERSHIP / ACCEPTANCE_AGREEMENT / CARD_ISSUANCE)`, `parties().registerCompany(…, ISSUER)`, `registerPerson(…, CARDHOLDER, cardUid)`, `cardsWithoutHolder().know` |
| M2 | których wydawców przyjmuje kemping słupka | jak M1 bez kart: słupek → kemping → region, wydawcy z nazwą (nazwa = `IssuerName`), członkostwo, umowy | jw. |
| M3 | które karty obsługi są na liście słupka | słupek → kemping → region; aktywne karty obsługi przypisane do kempingu albo regionu, z numerem = id `housecard` (HOUSE_CARD_NUMBER) | `registerPerson(…, STAFF_MEMBER, cardUid + houseCardNumber)`, `assign(kemping/region → osoba, STAFF_ASSIGNMENT)` |
| M4 | czy to karta sieci | karta → wydawca; wydawca należy (ISSUER_MEMBERSHIP\*) do Sieci (`party.network()`) | `assign(network, …)` / ISSUER_MEMBERSHIP do `network()` |

### Jednostki (co w starym modelu jest każdą z nich)
- **kemping**: grupa SITE, na której wiszą słupki (kandydat A); kandydat B: każda grupa SITE bez dzieci SITE. Skutek: A pomija puste kempingi (nikt o nie nie pyta), B tworzy kempingi bez słupków. Rekomendacja A + grupy SITE z umowami/kartami obsługi jako „miejsca” (region).
- **region**: grupa SITE ponad kempingiem (rodzic), poniżej korzenia rynku `<RYNEK> SITES`. Nowy model zna jeden poziom (kemping → region); głębsze drzewa to problem „układ”.
- **wydawca**: grupa ISSUER (każda, także podgrupy — podgrupa = jednostka wydawcy z ISSUER_MEMBERSHIP do rodzica). Korzeń `<RYNEK> ISSUERS` nie jest wydawcą (kandydat do sprawdzenia: czy ma umowy).
- **Sieć**: grupa ISSUER `NETWORK CARD` (i jej poddrzewo) → `party.network()` i jednostki Sieci.
- **karta gościa**: `camping_card` (nieskasowana, najnowsza dla UID) z gościem `guest` w grupie ISSUER; karta bez gościa → „bez posiadacza”.
- **karta obsługi**: `housecard` (nieskasowana, aktywna) w grupie SITE; id = HouseCardId.
- **słupek**: `pedestal` z grupą SITE (rzut na kemping).

### Cel trwały
Translator **przy starcie** (nowy model jest w pamięci, `PartyConfiguration.inMemory()`; komponent obok seedera, czyta stare tabele w jednej transakcji tylko do odczytu). Tabele nowego modelu odpadają: model nie ma persystencji (CLAUDE.md modułu).

### Porcja
Rynek **HR**: kempingi i regiony pod korzeniami SITE rynku HR (`HR SITES` i drugi korzeń `HR-NORTH`), wydawcy pod `HR ISSUERS`, ich goście i karty, Sieć (`NETWORK CARD`), karty obsługi w grupach SITE rynku HR. Wybór: rekurencyjne zejście od korzeni. Rozmiar (zapytanie przy wyborze porcji): HR SITES 235 grup / 547 słupków / 399 kart obsługi (+ 220 gości w grupach SITE), HR-NORTH 6 grup / 13 słupków / 6 kart obsługi, HR ISSUERS 314 grup / 55 795 gości. Login konsoli `admin` ma zakres HR.

## Słownik dwóch języków
| Stara tabela / kolumna | Znaczenie | Fakt nowego modelu albo „poza (dlaczego)” | plik:linia |
|---|---|---|---|
| `tbl_group` typ SITE z `pedestal.group_id` | kemping (miejsce ze słupkami) | OrganizationUnit ACCEPTOR | `campsite-core/src/main/java/io/pillopl/acceptance/LegacyCardAcceptance.java:43` |
| `tbl_group.parent_id` (SITE nad SITE) | region / rynek nad kempingiem, dowolna głębokość | REGION_MEMBERSHIP (nowy model: jeden poziom, `AcceptancePermissions.covers`) | `campsite-core/src/main/java/io/pillopl/group/GroupService.java:250` (fetchParentLine) |
| `pedestal.code`, `pedestal.group_id`, `pedestal.deleted` | słupek i jego kemping | `Pedestals.standsAt(code, kemping)`; skasowany słupek: M1 „nie” | `campsite-core/src/main/java/io/pillopl/acceptance/LegacyCardAcceptance.java:43` |
| `tbl_group` typ ISSUER, `name` | wydawca (nazwa = `IssuerName`) | Company / OrganizationUnit ISSUER, nazwa bez zmian | `campsite-core/src/main/java/io/pillopl/acceptance/LegacyCardAcceptance.java:58` |
| `tbl_group.parent_id` (ISSUER nad ISSUER) | organizacja nadrzędna wydawcy | ISSUER_MEMBERSHIP | `campsite-core/src/main/java/io/pillopl/group/GroupServiceHelper.java:89` |
| `group_associatedgroup` (ISSUER → SITE) | umowa akceptacji; najbliższa niepusta lista w górę zastępuje wyższe; rozwijana do poddrzewa | ACCEPTANCE_AGREEMENT + polityka `ownReplaceInherited` | `campsite-core/src/main/java/io/pillopl/group/GroupServiceHelper.java:97`, SQL `campsite-core/src/main/resources/sql/sqls.xml:36` |
| `group_associatedgroup` (ISSUER → ISSUER/STAFF) | wpis innego rodzaju | poza (reguła liczy tylko cele SITE) | `campsite-core/src/main/java/io/pillopl/group/GroupServiceHelper.java:97` |
| `tbl_group` ISSUER `NETWORK CARD` + poddrzewo | Sieć i jej jednostki | `party.network()` / ISSUER_MEMBERSHIP do Sieci | `campsite-core/src/main/java/io/pillopl/group/GroupService.java:218` |
| `camping_card.card_rfid`, `deleted`, `guest_id`; `guest.group_id` | karta gościa i wydawca (grupa gościa) | Person CARDHOLDER (CARD_UID) + CARD_ISSUANCE | `campsite-core/src/main/java/io/pillopl/persistence/dao/CampingCardDao.java:15` |
| `camping_card.guest_id IS NULL` | karta bez posiadacza | `CardsWithoutHolder` | `campsite-core/src/main/java/io/pillopl/group/GroupService.java:335` |
| `camping_card.status`, `valid_from/to` | ważność, blokada | poza (sprawdza `validateFor`, nie port) | `campsite-core/src/main/java/io/pillopl/card/ValidationService.java:325` |
| `housecard` (`active`, `deleted`, `group_id`, `id`, `house_card_type`) | karta obsługi kempingu i grup nad nim | Person STAFF_MEMBER z HOUSE_CARD_NUMBER = id + STAFF_ASSIGNMENT | `campsite-core/src/main/java/io/pillopl/card/HouseCardService.java:88`, `campsite-core/src/main/java/io/pillopl/persistence/dao/HouseCardDao.java:16` |
| `housecard.rfid` | UID karty obsługi | poza (pytania M1 dla karty domowej nie dochodzą do portu; CARD_UID u obsługi grozi kolizją z kartą gościa w `Parties.findOneBy`) | `campsite-party/src/main/java/io/pillopl/party/AcceptancePermissions.java:96` |
| `guest`, `userinfo`, atrybuty `grp_attr_*`, drzewo STAFF | dane osobowe, ustawienia, konsola | poza (żadne przepięte miejsce o nie nie pyta) | `specyfikacje/drzewo-grup/miejsca.md:15` |

## Translator
`campsite-seeder/src/main/java/io/pillopl/seeder/migration/`: `OldAcceptanceRows` (wiersze starych tabel), `AcceptanceTranslator.translate(rows, PartyConfiguration)` (czysta funkcja, polecenia nowego modelu jak w `PartyAcceptanceWorld`), `MigrationReport` (raport), `AcceptanceMigration` (profil `acceptance-migration`, `SmartLifecycle` faza 1, jedna transakcja read-only, JDBC; raport do `acceptance.migration.report`). Profil zastępuje `acceptance-seed` (D18): dane seedu leżą już w starej bazie i przechodzą jak każde inne. Test: `AcceptanceMigrationTranslatorTest` (5, mała porcja: region dwa poziomy nad kempingiem, słupek na regionie, jednostka wydawcy, Sieć z jednostką, ten sam UID dwa razy, karta obsługi na korzeniu rynku, wydawca innego rynku). Przy starcie: 304 ms, raport w `raport-przeniesienia.json`.

## Różnice
Przed migracją (seed Party, profil `acceptance-seed`, te same 249 pytań, `roznice/wajcha-0-przed-migracja.jsonl`): **0 zgodnych / 0 różnych / 234 nie znam**. Po migracji — obrót 1 (21:05–21:12, wajcha `party` + porównanie, login `admin`): 249 pytań (`roznice/requests.json` z `roznice/requests.sql`: M1 80 losowych + 46 „pod umową” + 32 przypadki problemów; M2+M3 60 losowych list + 31 list problemów). HTTP 200×225, 403×17, 404×7. Ekran wajchy (`roznice/wajcha-1.jsonl`): **234 zgodne / 0 różne / 0 nie znam**; plik różnic pusty (`roznice/grupy-1.json`: 0 grup). 24 pytania nie doszły do portu (P09, D20). Drugi obrót niepotrzebny. Uwaga: 249 pytań trwało 7 min (~1,7 s na pytanie) — model Party liczy M2 przez wszystkich 866 wydawców po liście ~64 tys. relacji (`PartyRelationships.from` to skan listy); na pełnych danych to problem wydajności nowego modelu, nie migracji.

## Kontrola
**Oczekiwane** (`kontrola/oczekiwane.sql` → `oczekiwane.jsonl`): jedno zapytanie wprost na starych tabelach, bez translatora i klas aplikacji — reguła umów przepisana z SQL starego systemu (`findAcceptingGroupIds` dla M1, `findGroupIdsAcceptedAt` dla M2), M3 = aktywne nieskasowane karty z linii rodziców grupy słupka, M4 = grupa gościa najnowszej karty w poddrzewie `NETWORK CARD`. Pytania: te same co w kroku 6 (158 par karta×słupek, 158 kart M4, 91 słupków M2 i M3) — także te, których aplikacja nie zadała (HR-NORTH, wydawca pod kempingiem). **Nowy** (`kontrola/nowy.jsonl`): odpowiedzi modelu Party zasilonego translatorem z tej samej bazy (`AcceptanceMigrationControlAnswers`, uruchamiany ręcznie z `-Dkontrola.jdbc`), w kształcie odpowiedzi portu (tak/nie, posortowane zbiory nazw/numerów).
Wynik: **480/496 takie same**; 16 różnych = 8 UID zdublowanych, których najnowsza karta jest wydawcy FR/IT, poza porcją → nowy „nie znam” (P10, D14; przy wajsze router pyta wtedy stary). W obrębie porcji **480/480** (`oczekiwane-porcja.jsonl` vs `nowy-porcja.jsonl`). Czas: 14 min, prawie cały na M2 w modelu Party (skan wszystkich relacji przy każdym `from`).

**Kontrola znaczenia** (zgodne liczbowo, a źle sklasyfikowane?):
- „NETWORK” w nazwie ≠ Sieć: IT/FR/ES NETWORK GUESTS leżą poza `NETWORK CARD` — obie strony mówią „nie karta sieci”; gdyby translator klasyfikował po nazwie, M4 rozjechałby się na 348 kartach. Sprawdzenie: M4 dla kart tych wydawców (w próbie są karty NETWORK HR GUESTS → tak).
- Region i kemping zlane w jedną grupę (P02): grupa ze słupkami i dziećmi jest w Party jednocześnie ACCEPTOR i REGION — odpowiedzi zgodne, ale model nie odróżnia „słupek na regionie” od „słupek na kempingu”; sprawdzić, czy biznes uznaje słupki na grupie-regionie (11 grup) za osobne kempingi.
- Karty RENTAL liczone jako karty obsługi (D12): M3 zgodne, bo nowy model dostał je jako obsługę; znaczeniowo to inna rola (wypożyczana karta recepcji) — porównanie tego nie wykryje.
- Spłaszczenie REGION_MEMBERSHIP na wszystkie poziomy (D06) daje te same zbiory, ale „umowa z rynkiem” (4 umowy z korzeniem) wygląda w Party jak umowa z regionem — przy zmianie zasięgu na STATIC/FOLLOWS_REGION trzeba to rozróżnić.
