# Kontekst biznesowy: sieć kempingów sprzedająca prąd na stanowiskach

Plik opisuje, czym ten system jest jako biznes, językiem domeny. Nie zawiera celu analizy ani kandydatów. Każde zdanie ma źródło.

## Legenda źródeł

| Klucz | Źródło | Jak traktować |
|---|---|---|
| `[R]` | README repozytorium, napisany przez programistów | opis systemu słowami systemu; fakt o tym, co zbudowano, nie o tym, czego chce biznes |
| `[C]` | tematy commitów napisane jako historyjki użytkownika („As a … I can …”), 2012–2017 | deklaracja zamiaru spisana przez programistę; hipoteza o intencji biznesu |
| `[K]` | etykiety zakładek i ról konsoli operacyjnej | słowa systemu, które widzi personel |
| `[T: …]` | transkrypty rozmów z ludźmi biznesu | **brak** w tym przebiegu |
| `[Z]` | treść ticketów (CAMP-, PED-, ISS-, OPS-, CRD-, BIL-, RPT-, QRP-, SUP-, MIG-, PWR-, HR-) | **niepobrana**; tylko identyfikatory w commitach |

## Co robi ten biznes i dla kogo

Sieć kempingów sprzedaje energię elektryczną gościom na stanowiskach kempingowych `[R]`. Sieć działa na czterech rynkach: Chorwacja, Włochy, Francja i Hiszpania; rynki dzielą się na regiony i operatorów kempingów `[R]`. Na stanowiskach stoją słupki z gniazdami z licznikiem; gość podłącza się kartą kempingową, kartą domową kempingu albo płatnością QR w aplikacji `[R]`. Sieć wystawia gościom rachunki przez system ERP, wymienia karty i rekordy z partnerskimi wystawcami kart, pilnuje słupków i budżetu mocy sekcji kempingu `[R]`.

## Strony i czego chcą

- **Gość** chce włączyć i wyłączyć prąd przy swoim gnieździe `[C: CAMP-73]`. Gość przejazdowy chce zapłacić przy gnieździe kodem QR i wcześniej zobaczyć szacunek ceny `[C: CAMP-97]`. Gość może być zameldowany na pobyt; wymeldowanie zamyka jego dostawy `[C: PED-50, OPS-34]`.
- **Recepcja** chce meldować i wymeldowywać gości, szukać ich po nazwisku, numerze rejestracyjnym lub karcie, blokować karty i zmieniać ich ważność, wydawać kartę powitalną z puli na pobyt `[C]`. Recepcja widzi pulpit, mapę, karty, gości i pobyty oraz sesje dostaw `[R][K]`.
- **Gospodarz kempingu** (kierownik) chce widzieć kluczowe liczby i alerty na żywo, obciążenie sekcji w czasie, zużycie na godzinę i dzień tygodnia, eksportować raporty, przełączać tryb dostępu swojego kempingu i chce, by dostawy były odrzucane, gdy budżet sekcji jest pełny `[C: CAMP-141, CAMP-15]`. Widzi taryfy i raporty swojego kempingu `[R]`.
- **Właściciel kempingu / afiliant** chce zarządzać kartami domowymi swojego kempingu i chce, by karta powitalna z puli była przyjmowana przy pierwszym użyciu `[C: CRD-22, CAMP-44]`.
- **Technik serwisu** chce, by milczące słupki były oznaczane, i chce dostawać maila, gdy gniazdo zgłasza błąd `[C: OPS-21]`. Widzi mapę, słupki i gniazda, budżet mocy oraz monitoring i serwis `[R][K]`.
- **Back office** chce zatwierdzać lub odrzucać podejrzane rekordy zużycia, budować taryfy i symulować ceny, sprzedawać sezonowe pakiety mocy, ustawiać filtry białej listy dla partnerów-wystawców, podglądać i uruchamiać rozliczenie ERP za miesiąc, importować słupki dostawcy z pliku `[C: OPS-34, QRP-10, PED-48]`. Widzi rekordy zużycia, taryfy, płatności QR, raporty, rozliczenie ERP i wymianę z wystawcami `[R][K]`.
- **Administrator rynku** chce tworzyć grupy podrzędne w drzewach organizacji i przeszukiwać ślad audytu `[C: CAMP-13]`. Widzi wszystko w swoim rynku i administrację `[R]`.
- **Wystawca kart** (w tym partner zewnętrzny) chce wystawiać i blokować karty kempingowe i chce co noc znać stan synchronizacji swoich kart `[C: CAMP-122]`. Jego lista umów ma zastępować listy wyżej w drzewie `[C: CAMP-13]`. Z partnerem wymienia się pliki kart i rekordów zużycia, także importuje się rekordy zużycia partnera `[C: PED-63, ISS-6]`.
- **Słupek** (sterownik) chce odpowiedzi, czy karta może pobierać prąd, chce listy lokalnej kart mieszczącej się w jego pamięci i chce, by znaleziono bieżącą sesję jego gniazda `[C: CRD-22, CAMP-73]`.
- **Dostawca słupków** dostarcza słupki, które przechodzą ze stanu magazynowego przez uruchomienie, a mogą wrócić do dostawcy `[C: PED-48]`.

## Co jest sprzedawane i kto się z kim rozlicza

Sprzedawana jest energia pobrana w sesji dostawy; z sesji powstaje wyceniony rekord zużycia `[R]`. Wycena opiera się na taryfach z ograniczeniami czasu, dnia i kWh oraz na sezonowych umowach z pakietami mocy `[R]`. Rachunki dla gości sieci idą przez ERP; miesięcznie powstają linie ERP na konto rozliczeniowe `[R][C: PED-63]`. Sezonowa opłata stała poniżej minimalnej kwoty rachunku ma być fakturowana `[C: ISS-20]`. Zatrzymanie dostawy kartą domową jest księgowane jako niefakturowane `[C: SUP-8]`. Rekordy importowane od partnera zachowują koszt partnera przez przegląd `[C: BIL-25]`. Numer partnera rozliczeniowego pochodzi od najbliższego właściciela kempingu `[C: PED-48]`. Rekord zużycia może być „podejrzany” według reguły i wtedy czeka na przegląd recenzenta, który dostaje maila `[C: BIL-22][C]`.

## Codzienne sytuacje

- Gość przykłada kartę do słupka; słupek pyta sieć, czy karta może pobierać prąd; odpowiedź zawiera nazwisko gościa i datę ważności karty `[C: CRD-22]`.
- Gość przejazdowy skanuje QR, widzi szacunek, płaci w aplikacji i prąd rusza zdalnie `[C: CAMP-97][R]`.
- Gość wyjeżdża bez wyłączenia prądu; po wymeldowaniu sieć zamyka jego sesje `[C: PED-50]`.
- Słupek milczy; technik chce to zobaczyć na liście `[C: OPS-21]`. Słupek wysyła ten sam start ponownie, bo nie dostał odpowiedzi `[kod: komentarz w logice startu, nie potwierdzone biznesowo]`.
- Rekord zużycia wychodzi podejrzany (zbyt dużo kWh na noc); back office go zatwierdza lub odrzuca `[C: OPS-34][R]`.
- Pod koniec miesiąca back office podgląda i uruchamia rozliczenie ERP `[C]`.
- Co noc wymienia się pliki z partnerami-wystawcami; partner dostaje stan synchronizacji swoich kart `[C: CAMP-122, ISS-6]`.
- Sekcja mocy kempingu jest pełna; nowa dostawa ma być odrzucona `[C]`.
- Nowy słupek przychodzi od dostawcy i jest uruchamiany na kempingu `[C: PED-48]`.

## Słowa domeny

| Słowo | Rodzaj | Źródło | Uwagi |
|---|---|---|---|
| kemping, stanowisko (pitch) | business | `[R][C]` | miejsce, gdzie stoi gość |
| słupek (pedestal), gniazdo (socket) | system | `[R]` | urządzenie z licznikiem; „trzy generacje łącza” to słowo systemu |
| karta kempingowa | business | `[C]` | wystawiana przez wystawcę, ma gościa, ważność, może być blokowana |
| karta domowa (house card) | business? | `[C: CRD-22]` | karta kempingu do własnego użytku; „wynajem” karty domowej nie wyjaśniony |
| karta powitalna z puli | business? | `[C: CAMP-44, PED-80]` | przyjmowana przy pierwszym użyciu; kto ją funduje – nieznane |
| płatność QR, gość przejazdowy | business | `[C: CAMP-97]` | płatność bez karty przez aplikację |
| sesja dostawy (supply session) | system | `[R][C]` | od startu do zatrzymania prądu |
| rekord zużycia | system | `[R][K]` | wyceniona sesja; może być podejrzany, w przeglądzie, eksportowany |
| taryfa, pakiet mocy, umowa sezonowa | business | `[R][C: QRP-10]` | ograniczenia czasu, dnia, kWh |
| budżet mocy sekcji | business? | `[C][K]` | limit obciążenia części kempingu |
| wystawca kart, partner-wystawca, umowa akceptacji | business | `[R][C]` | drzewo wystawców z podgrupami; kto płaci komu – nieznane |
| tryb dostępu kempingu | system? | `[C: CAMP-15]` | przełącza właściciel; znaczenie biznesowe nieznane |
| biała lista (whitelist filter), lista lokalna | system | `[C]` | filtry kart dla partnerów i słupków |
| rozliczenie ERP, linia ERP, konto rozliczeniowe, numer partnera | business | `[R][C]` | miesięczne, przez zewnętrzny ERP |
| rynek, region, operator, drzewa SITE / ISSUER / STAFF | system | `[R]` | organizacja sieci |
| pobyt (stay), meldunek, wymeldowanie, jednostka z tablicą rejestracyjną | business | `[C]` | przyczepa/kamper gościa |
| incydent dostawy, przegląd incydentu | system | `[C: CAMP-141]` | zatrzymanie, którego sieć nie umie zaksięgować |

## Nie wiadomo

- **Kto płaci komu.** Czy gość płaci kempingowi, sieci czy wystawcy karty; co znaczy „rozliczany przez kemping” przy słupku; jak partner-wystawca rozlicza się z siecią za rekordy importowane i eksportowane `[brak źródła]`.
- **Czym jest „rynek” biznesowo** (spółka, kraj, waluta?) i dlaczego kod wystawcy buduje się z rynku `[C: bf1d427, bez ticketu]`.
- **Kto funduje karty powitalne** i czym jest „adopcja” karty przy pierwszym użyciu `[C: CAMP-44]`.
- **Co znaczy tryb dostępu kempingu** i kiedy właściciel go zmienia `[C: CAMP-15]`.
- **Czym są role nieopisane**: „afiliant/właściciel afiliacyjny” vs „operator kempingu” vs „gospodarz”; czy to te same osoby `[R][C]`.
- **Dlaczego trzy generacje łącza słupków istnieją równolegle** i czy generacja pierwsza nadal działa w terenie `[R]`.
- **Co boli dziś.** Brak ticketów i incydentów; znaczenie prefiksów ticketów (CAMP, PED, ISS, OPS, CRD, BIL, RPT, QRP, SUP, MIG, PWR, HR) nieznane `[Z]`.
- **Jakie funkcje są planowane** – cel mówi „nowe funkcje mają wchodzić szybciej”, ale nie wiadomo jakie `[brak źródła]`.
- **Reguła opisana numerem ticketu w logice autoryzacji** (CAMP-84) – treść ticketu nieznana `[Z]`.
- **Czy daty historii są prawdziwe**: commity 2012–2017, potem od razu 2026 `[art-git]`.
