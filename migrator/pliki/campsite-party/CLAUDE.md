# campsite-party — hipoteza: archetyp Party obok starego modelu

**Czym jest:** minimalna implementacja archetypu Party (podmioty, role, identyfikatory, relacje z ważnością
i uprawnienia wyprowadzane z relacji) odpowiadająca na cztery pytania z `specyfikacje/drzewo-grup/miejsca.md`:
M1 czy karta działa na słupku (CARDS_ACCEPTED), M2 których wydawców przyjmuje kemping słupka, M3 które karty obsługi
są na liście słupka (CHARGE_AS_STAFF), M4 czy to karta sieci. Stoi obok legacy za portem
`io.pillopl.acceptance.CardAcceptance` (campsite-core); zna go tylko adapter `PartyCardAcceptance` i świat
`PartyAcceptanceWorld`. To hipoteza do sprawdzenia scenariuszami (`AcceptanceScenarios`), nie docelowy model.

**Skąd:** `specyfikacje/drzewo-grup/zrodlo.md` (pogłębienie Party z `analiza/archetypy.md`), sekcje „Nowe
rozwiązanie” i „Słowniki” specyfikacji; wzór klas: repo Archetypy-Oprogramowania/archetypes, moduł `party`
(PartyId, Party, Role, RegisteredIdentifier, PartyRelationship, Validity, `PartyConfiguration.inMemory()`).

**Mówi językiem:** podmiot (osoba, firma, jednostka organizacyjna), rola (ISSUER, ACCEPTOR, REGION, CARDHOLDER,
STAFF_MEMBER, NETWORK_OPERATOR), identyfikator (CARD_UID, HOUSE_CARD_NUMBER), relacja z ważnością
(ACCEPTANCE_AGREEMENT, ISSUER_MEMBERSHIP, CARD_ISSUANCE, STAFF_ASSIGNMENT, REGION_MEMBERSHIP), Sieć jako podmiot,
słupek tylko jako rzut na kemping, „nie znam” (`Answer.NotKnown`) osobno od „nie”.

**Nie mówi językiem legacy:** group, tree, parent, ancestor, subtree, children, prefix, nearest, attribute, typ
grupy SITE / STAFF, prefiks AFFILIATE, NETWORK CARD, AccessMode, HOUSE_CARDS_ONLY, NETWORK_ACCESS, whitelist,
LocalList, manageable, synchronizable; po polsku: grupa, drzewo, rodzic, przodek, poddrzewo, prefiks, atrybut,
najbliższy w górę. Tych słów tu nie ma i nie ma ich być.

**Stare zachowanie jako polityki** (wstrzykiwane w `PartyConfiguration`, źródło mówi inaczej — do potwierdzenia):
- `AgreementsOfIssuer.ownReplaceInherited` — własne umowy jednostki wydawcy zastępują umowy organizacji (źródło:
  sumują się);
- `StaffAssignmentReach.campsiteAndItsRegion` — przypisanie obsługi do regionu działa na kempingach regionu
  (źródło: tylko kemping przypisania).

**Zasady:**
- Nie importuje niczego z `io.pillopl` poza własnym pakietem (nawet typów wartości — tłumaczy je adapter).
- Dane w pamięci; bez persystencji, migracji i komend biznesowych (tylko to, czego potrzebuje świat i pytania).
- Poza modelem: ważność i blokada karty, lista lokalna (pojemność, kolejność, rozsyłka), wymiana z partnerami,
  przygarnianie kart powitalnych, kemping afiliowany, zasięg umowy STATIC.
