---
obszar: drzewo-grup
krok: swappable-dsl.2
---

# Miejsca: drzewo-grup

Kontrakt miejsc. Od kroku 3 workflow `parallel-models` pracuje tylko na tych miejscach: nie wraca do kandydatów ani do
źródła (crossing, mapper). Co najmniej 3 miejsca w kodzie (wołający), łatwe do przepięcia na nowe API.

Wybrane: 4 miejsca u 3 wołających (M2 i M3 to dwa pytania tego samego wołającego, na tym samym wejściu do porównania).
Ścieżki skrótem: `…/card/` = `campsite-core/src/main/java/io/pillopl/card/`, `…/issuer/` =
`campsite-core/src/main/java/io/pillopl/issuer/`.

| # | CO (pytanie wołającego) | Wołający (plik:linia) | JAK stare | JAK w nowym rozwiązaniu | Odpowiedź | Skąd | Dlaczego łatwe | Wejście do porównania | Status |
|---|---|---|---|---|---|---|---|---|---|
| M1 | Gość przykłada kartę do gniazda: czy wydawca jego karty ma umowę obejmującą kemping, na którym stoi słupek? | `ValidationService.validateFor` (`…/card/ValidationService.java:329`) przez prywatne `isCardAcceptedAt` (:381) | `groupService.isCardAcceptedAt(pedestalId, card)`: karta bez gościa → nie; najbliższa niepusta lista umów nad wydawcą gościa, rozwinięta do całych poddrzew; słupek na liście | CARDS_ACCEPTED: CARD_ISSUANCE (UID → posiadacz → wydawca) → ISSUER_MEMBERSHIP\* → ACCEPTANCE_AGREEMENT obejmuje kemping słupka (rzut słupek → kemping) | tak/nie (`boolean`) | kandydat K1; B1, B3; Q1 | wołający w kontenerze (jeden konstruktor `@Service`), pytanie w jednej linii (:329), parametry = typy wartości projektu `CardRfid`, `PedestalCode`, odpowiedź tak/nie | konsola: `POST /checkCardAtSocket` (`CardWebService` → `CardApiService.checkCardAtSocket`), status `CARD_NOT_ACCEPTED_AT_SITE` vs dalsze statusy | wybrane |
| M2 | Słupek dostaje listę kart do wpuszczania bez pytania sieci: karty których wydawców przyjmuje kemping tego słupka? | `LocalCardListService.buildLocalCardList(Pedestal, LocalListAttribute)` (`…/card/LocalCardListService.java:151`) | `groupService.findIssuerGroupIdsAcceptedAt(grupa słupka)` (rekurencyjny SQL), wynik idzie do `campingCardDao.findValidCardsForLocalCardList(ids, dziś)` | odwrotnie po ACCEPTANCE_AGREEMENT (kemping albo jego region) i w dół po ISSUER_MEMBERSHIP: wydawcy, których karty kemping przyjmuje | krótka lista wydawców (identyfikatory; wołający dociąga karty jak dotąd) | kandydat K2a; B1, B3; Q2 | wołający w kontenerze, pytanie w jednej linii, parametr = kemping słupka (`PedestalCode`), lista bez kolejności (porównanie jako zbiór) | konsola: `POST /getLocalCardList` (podgląd bez wysyłki, `CardApiService.getLocalCardList`), karty gości na liście | wybrane |
| M3 | Słupek dostaje listę kart do wpuszczania bez pytania sieci: które karty obsługi mają na niej być? | `LocalCardListService.buildLocalCardList(Pedestal, LocalListAttribute)` (`…/card/LocalCardListService.java:144`) | `houseCardService.getHouseCardsForPedestal(kod)`: aktywne karty domowe kempingu słupka i wszystkich części sieci nad nim | aktywne STAFF_ASSIGNMENT do kempingu słupka (CHARGE_AS_STAFF) | krótka lista kart obsługi (identyfikatory `HouseCardId`; wołający dociąga karty jak dotąd, kolejność i ucinanie bez zmian) | kandydat K2b; B1, B3; Q4, Q12 | ten sam wołający co M2, pytanie w jednej linii, parametr `PedestalCode`, wynik → typ wartości `HouseCardId` | jak M2: `POST /getLocalCardList`, karty obsługi na liście | wybrane |
| M4 | Partner przysyła zmianę karty, którą już znamy: czy to karta sieci? Tak → zmiana odrzucona | `IssuerCardImportService.importCard` (`…/issuer/IssuerCardImportService.java:81`) przez `isNetworkCard` (:187) | karta bez grupy gościa → nie; grupa gościa w poddrzewie grupy `NETWORK CARD` | CARD_ISSUANCE dla CARD_UID, w której wydawcą jest Sieć | tak/nie (`boolean`) | kandydat K5; Q16 | wołający w kontenerze (jeden konstruktor `@Service`), pytanie w jednej linii (:81), parametr `CardRfid`, odpowiedź tak/nie, sprawdzenie przed resztą kroków (kolejność zachowana) | wymiana z partnerem: `POST /runIssuerExchange` z plikiem kart partnera zawierającym linię z UID karty sieci; status wymiany (odrzucenie „exists as a network card already”) — odrzucona linia nie zmienia bazy, więc powtarzalne | wybrane |

## Odrzuceni kandydaci
- **K3** `WelcomeCardAdoptionService.checkPedestal` — pytanie zadawane w tej samej klasie co wołający `checkAndAdopt`,
  który zaraz potem drugi raz pyta stare drzewo o właściciela (`getAffiliateOwner`, z nazwy) — wpięcie samego
  `checkPedestal` zostawia połowę pytania w starym modelu; jedyne wejście do porównania to przyłożenie karty na słupku
  i zdarzenie asynchroniczne z efektem ubocznym (karta przerobiona, powitalna skasowana) — niepowtarzalne.
- **K4** `IssuerExchangeService.exportCards` — parametr partnera to encja `IssuerPartner` bez typu wartości, pytanie
  w podwójnej pętli w transakcji; jedyne wejście do porównania to wymiana, która oznacza karty jako wyeksportowane
  (zmienia bazę) — niepowtarzalne.
