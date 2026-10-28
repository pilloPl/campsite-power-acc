# Polecenia świata nowego modelu (AcceptanceWorld → PartyAcceptanceWorld)

Jedyny sposób zakładania faktów w nowym modelu. Nazwy są kluczami: polecenie odwołuje się do kempingu, regionu albo
wydawcy po nazwie, którą nadało mu wcześniejsze polecenie (albo seed, albo wcześniejsze wgranie). Nazwa podmiotu =
nazwa grupy z legacy.

| op | pola | co zakłada w Party |
|---|---|---|
| `region` | name | miejsce z rolą REGION (grupa nad kempingiem: region, podregion, korzeń rynku) |
| `campsite` | name | kemping (ACCEPTOR) bez członkostwa w regionie |
| `campsiteIn` | name, region | kemping (ACCEPTOR) + jedno REGION_MEMBERSHIP kemping → region |
| `campsiteAndRegion` | name | miejsce, które jest naraz kempingiem i regionem (ACCEPTOR + REGION): ma słupki i miejsca pod sobą |
| `memberOfRegion` | name, region | kolejne REGION_MEMBERSHIP miejsca (kempingu albo regionu) do regionu - np. do każdego przodka przy spłaszczonym drzewie |
| `pedestal` | code, campsite | rzut słupka na kemping (słupek nie jest podmiotem; kemping musi mieć rolę ACCEPTOR) |
| `issuer` | name | firma z rolą ISSUER (wydawca kart) bez organizacji nad nią |
| `issuerWithin` | name, organization | jednostka wydawcy (ISSUER) + ISSUER_MEMBERSHIP jednostka → organizacja; bez własnych umów przejmuje umowy organizacji |
| `issuerWithinTheNetwork` | name | jednostka sieci jako wydawca + ISSUER_MEMBERSHIP do Sieci („THE NETWORK”) |
| `agreementWithCampsite` | issuer, campsite | ACCEPTANCE_AGREEMENT wydawca → kemping |
| `agreementWithRegion` | issuer, region | ACCEPTANCE_AGREEMENT wydawca → region (obejmuje kempingi, które mają REGION_MEMBERSHIP do tego regionu) |
| `guestCard` | uid, issuer | osoba (CARDHOLDER) znana po CARD_UID + CARD_ISSUANCE wydawca → osoba |
| `networkCard` | uid | jak guestCard, wydawcą jest sama Sieć |
| `cardWithoutHolder` | uid | karta znana sieci, nikomu niewydana |
| `staffCard` | houseCardNumber, name | osoba (STAFF_MEMBER) z HOUSE_CARD_NUMBER (bez UID) + STAFF_ASSIGNMENT miejsce → osoba; działa na kempingu `name` i na kempingach, które mają REGION_MEMBERSHIP do `name` |

Jak nowy model liczy zasięg: umowa i przypisanie obsługi sięgają JEDEN skok - kemping słupka albo miejsca, do których
kemping ma REGION_MEMBERSHIP. Żeby umowa z korzeniem rynku albo z regionem dwa poziomy wyżej działała jak w legacy, kemping
musi mieć członkostwo do każdego przodka (polecenia memberOfRegion), jeśli decyzja tak każe.
Polecenia odwołują się do miejsc i wydawców po nazwie; najpierw załóż to, do czego się odwołujesz (przodków od góry).

Czego nowy model NIE ma: przeniesienia kempingu do innego regionu (zakończenia członkostwa), dodania roli istniejącemu
podmiotowi, drugiego posiadacza tego samego UID, ważności karty i blokady, listy lokalnej, wymiany z partnerami,
kempingu afiliowanego, przygarniania kart powitalnych, taryf i rozliczeń.

Polityki dzisiejszego zachowania (wstrzyknięte w PartyConfiguration, nie zmieniamy ich): własne umowy jednostki
wydawcy zastępują umowy organizacji (`AgreementsOfIssuer.ownReplaceInherited`); przypisanie obsługi do regionu
działa na kempingach regionu (`StaffAssignmentReach.campsiteAndItsRegion`).

Wgranie odbywa się polecenie po poleceniu. Polecenie, które nowy model już zna tak samo, jest pomijane („już jest”).
Polecenie sprzeczne z tym, co model wie (UID ma innego posiadacza, kemping jest w innym regionie, nazwa oznacza dwa
podmioty), jest problemem i nie wchodzi; propozycja z problemem nie powstaje.

# Tabele legacy (tylko do odczytu, narzędzie `sql`)

- `v_group(id, name_path, lvl, typename, …)` — widok ze ścieżką nazw od korzenia (Kamil go używa).
- `tbl_group(id, parent_id, name, typename, displayname)` — drzewa grup; `typename` ISSUER | SITE | STAFF; (name, typename) unikalne.
  Drzewo SITE: korzeń rynku („HR SITES”) → region → (czasem grupa pośrednia, np. operator) → kemping → (czasem sekcja).
  Drzewo ISSUER: korzeń rynku wydawców („HR ISSUERS”) → wydawca → jednostki (dowolnie głęboko); grupa „NETWORK CARD” = karty sieci.
- `grp_attr_site_tenant(group_id, name)` — najemca kempingu: grupa SITE z wierszem tutaj to kemping.
- `group_associatedgroup(group_id, associatedgroup_id)` — umowa akceptacji: group_id = grupa wydawcy, associatedgroup_id = grupa SITE (kemping, region, …).
- `pedestal(id, code, group_id, deleted, …)` — słupek; group_id = grupa SITE (może być pusta).
- `guest(id, group_id, name, deleted, …)` — gość; group_id = grupa wydawcy jego karty.
- `camping_card(id, card_rfid, card_number, guest_id, status, valid_from, valid_to, deleted, …)` — karta gościa; UID = card_rfid (bywa na kilku kartach).
- `housecard(id, group_id, house_card_type, rfid, card_number, active, deleted, …)` — karta obsługi (STAFF) albo wypożyczana (RENTAL); id = numer karty obsługi w M3.

Pytania przepiętych miejsc w legacy: M1 = czy karta (card_rfid) działa na słupku (code); M2 = nazwy wydawców przyjmowanych na słupku;
M3 = numery (housecard.id) kart obsługi na liście słupka; M4 = czy karta jest kartą sieci.
