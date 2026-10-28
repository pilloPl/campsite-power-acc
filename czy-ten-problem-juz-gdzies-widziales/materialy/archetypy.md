# Archetypy: „Czy ta karta dostanie prąd na tym kempingu?”

Skan archetypów (skill `archetype-storyteller`, tryb skan), 2026-10-25. Skan nie widział kodu: dostał problem w języku biznesu i kontekst systemu z `analiza/business-context.md`. Dziewięciu agentów fit-check, każdy osobno dla jednego archetypu.

## Wejście skanu

Dosłownie to, co dostał każdy agent skanu: problem (plik `analiza/_problem-wejscie.md`) i kontekst systemu (plik `analiza/business-context.md`). Nagłówki obniżone.

### PROBLEM

Paczka pytań: Czy ta karta dostanie prąd na tym kempingu?
Co się zmienia: w gnieździe płynie prąd albo nie. Kto zauważy: gość przy słupku, personel po odmowie.

Pytanie 1 (Q1): Czy wydawca karty gościa ma z siecią umowę, że jego karty działają na kempingu, na którym stoi ten słupek?
Podsumowanie: Naprawdę pyta tylko słupek, gdy gość przykłada kartę: przy autoryzacji, starcie dostawy, zdalnym starcie z konsoli, próbie na sucho operatora i przy zatrzymaniu cudzą kartą. Ekran umów wydawcy nie pyta o konkretny słupek, tylko pokazuje operatorowi, na których kempingach działają karty wydawcy. Trzy wejścia po stronie kempingu i kodu słupka istnieją, ale w działaniu sieci nikt ich nie używa.
- Operator sieci otwiera umowy wydawcy kart, dodaje albo usuwa umowę z kempingiem i chce zobaczyć, na których kempingach karty tego wydawcy teraz działają i czy te umowy są jego własne, czy przejęte od wydawcy wyżej. Nie pyta o jeden słupek, pyta o cały zasięg umów wydawcy.
- Na których kempingach działają karty tego wydawcy, wliczając kempingi pod kempingami z umowy — to wspólna odpowiedź, z której korzysta słupek przy karcie gościa i ekran umów operatora; samo nie ma swojej sytuacji.
- Gość przykłada kartę do gniazda słupka (albo operator z konsoli uruchamia prąd lub sprawdza kartę na gnieździe): czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza.

Pytanie 2 (Q2): Karty których wydawców przyjmuje kemping tego słupka?
Podsumowanie: Jedno pytanie zadawane, gdy słupek dostaje pełną lokalną listę kart (z konsoli albo przy rozsyłce do wszystkich słupków) i gdy personel ją tylko podgląda. Odpowiedź to wydawcy, których umowa obejmuje kemping słupka lub coś wyżej w drzewie.
- Słupek dostaje pełną listę kart, które rozpozna bez sieci (przy rozsyłce do słupków albo na żądanie z konsoli), albo personel ją podgląda: karty których wydawców przyjmuje kemping tego słupka? Karty innych wydawców nie trafiają na listę, więc bez łączności słupek ich nie wpuści.

Pytanie 3 (Q3): Na których kempingach mogą ładować karty tego wydawcy (tego gościa) i czy to umowa wydawcy, czy odziedziczona po organizacji nadrzędnej?
Podsumowanie: Jedyny żywy przypadek to ekran umów w konsoli: operator sprawdza, na których kempingach działają karty wydawcy i czy to jego własna umowa, czy przejęta po organizacji nadrzędnej (także zaraz po dodaniu lub zdjęciu umowy). Wersja „na których słupkach w moim zakresie gość może ładować” istnieje w kodzie, ale nikt jej nie woła.
- Operator w konsoli otwiera umowy wydawcy: na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej? Wydawca spoza jego zakresu albo coś, co nie jest wydawcą: odmowa.
- Operator dodał albo zdjął umowę wydawcy z kempingiem (albo tylko ją podgląda): na których kempingach karty tego wydawcy działają teraz i czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?

Pytanie 4 (Q4): Czy to karta personelu, której wolno ładować na tym kempingu bez ograniczeń (i dla jakiego obszaru ją wydano)?
Podsumowanie: Pytanie zadaje słupek przy każdym przyłożeniu karty (start ładowania, zatrzymanie, zdalny start, sprawdzenie przez personel): karta personelu kempingu lub jego regionu ładuje od razu, bez reszty sprawdzeń. Odpowiedź z góry dostaje też słupek w swojej lokalnej liście kart. Kto ma tę odpowiedź, ustalają dwa miejsca: operator kempingu, który wydaje kartę personelu, i przejęcie karty powitalnej gościa, po którym ta karta staje się kartą personelu.
- Gość albo pracownik przykłada kartę do gniazda słupka i chce ładować: czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu, nawet przy zajętym gnieździe i wyczerpanej mocy sekcji. Jeśli nie, karta przechodzi zwykłe sprawdzenia gościa.
- Operator kempingu zakłada lub zmienia kartę personelu w konsoli. Nie pyta, czy karta jest kartą personelu, tylko ustala odpowiedź: od teraz ta karta ładuje bez ograniczeń na całym jego kempingu (lub przestaje, gdy ją wyłączy). Słupki kempingu mają się o tym dowiedzieć.
- Słupek dostaje z góry listę kart, które może przyjąć bez łączności z siecią: które karty personelu kempingu i jego regionu ma znać? Karta personelu, której nie ma na liście, nie zadziała przy braku łączności. Gdy kart personelu jest więcej niż miejsca, karty gości wypadają z listy.
- Gość z kartą powitalną sieci został odrzucony na słupku kempingu partnerskiego, który wpuszcza tylko karty personelu. Czy kemping zgadza się przejąć tę kartę? Jeśli tak, karta gościa staje się kartą personelu kempingu i odtąd ładuje tam bez ograniczeń. Tu nikt nie pyta, czy to karta personelu, tylko się ją nią robi.
- Przy każdym sprawdzeniu karty na słupku (start ładowania, zatrzymanie inną kartą, zdalny start z konsoli, próbne sprawdzenie przez personel): czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, jest przyjęta od razu, bez sprawdzania, czy kemping przyjmuje karty jej wydawcy i czy karta jest ważna.

Pytanie 5 (Q10): Czy kemping chce przejąć kartę powitalną, którą gość zostawił, jako własną kartę obsługi — i czy to w ogóle karta powitalna z puli tego kraju?
Podsumowanie: Pytanie zadaje naprawdę tylko jedno miejsce: zaraz po tym, jak słupek odmówił karcie, bo kemping jej nie przyjmuje, system w tle sprawdza, czy to karta powitalna z puli kraju słupka i czy kemping afiliowany (tylko karty domowe, z włączonym przejmowaniem) ją przejmie. Ekran grupy pokazuje operatorowi tylko ustawienie „przejmowanie dozwolone”, a recepcja korzysta z tej samej puli w innym celu: sprawdza, czy kartę z puli dowolnego kraju można wydać gościowi.
- Gość przykłada kartę do słupka i dostaje odmowę, bo kemping tej karty nie przyjmuje: czy warto w tle sprawdzić, czy kemping przejmie ją jako własną kartę obsługi? Inne odmowy (np. karta nieważna, gniazdo zajęte) niczego nie uruchamiają.
- Operator ogląda ustawienia grupy (kempingu, regionu), którą zarządza: czy przejmowanie porzuconych kart powitalnych jest tu włączone (wprost albo odziedziczone z wyższego poziomu)? Widzi tylko ustawienie, nie to, czy przejęcie by się udało (kraj karty, tryb tylko karty domowe, afiliacja).
- Słupek właśnie odmówił karcie, bo kemping jej nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu, a kemping afiliowany przyjmuje tylko własne karty i pozwala przejmować porzucone karty powitalne? Jeśli tak, karta staje się kartą obsługi właściciela kempingu i znika z puli; jeśli nie, nic się nie dzieje.

Pytanie 6 (Q11): Czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi?
Podsumowanie: Tylko jedno miejsce naprawdę pyta, czy niezależny kemping przyjmuje wyłącznie własne karty: odrzucenie karty powitalnej gościa na słupku, które może zamienić ją w kartę obsługi kempingu. Reszta to ekran atrybutów kempingu (pokazanie odpowiedzi) i dwa miejsca, które odpowiedź ustawiają (operator w konsoli i instalator przy uruchamianiu słupków) — przestawienie trybu przenosi kemping w drzewie i zmienia widoczność słupków w aplikacji.
- Instalator kończy uruchamianie słupków na niezależnym kempingu i ustawia, czy kemping ma być otwarty na karty sieci, czy przyjmować tylko własne karty obsługi. Nie pyta o odpowiedź, tylko ją wyznacza; pyta jedynie, czy kemping ma już jakikolwiek tryb. Jeśli nie, uruchomienie kończy się sukcesem, a tryb po cichu się nie zmienia.
- Operator w konsoli przestawia niezależny kemping między „otwarty na karty sieci” a „tylko własne karty obsługi”. Nie pyta o odpowiedź, tylko ją zmienia; pyta jedynie, czy to w ogóle kemping niezależny z ustalonym trybem. Jeśli nie, dostaje odmowę.
- Operator (albo wsparcie) ogląda atrybuty kempingu w konsoli: czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi? Jeśli tryb nie daje się ustalić, pole jest puste i nie wiadomo dlaczego.
- Gość przykłada do słupka kartę powitalną z puli rynku, a kemping ją odrzucił: czy ten niezależny kemping przyjmuje tylko własne karty obsługi (i zgodził się przygarniać karty powitalne)? Jeśli tak, karta staje się kartą obsługi tego kempingu i znika z puli powitalnej; jeśli kemping jest otwarty na karty sieci, karta zostaje odrzucona i nic się nie dzieje.

Pytanie 7 (Q12): Czy ten słupek ma pamiętać listę kart, które może przyjąć bez pytania centrali, i jakie karty ma na niej trzymać (karty obsługi, karty gości)?
Podsumowanie: Pytanie pada w trzech sytuacjach: operator podgląda w konsoli, co słupek dostanie; słupek dostaje pełną listę (ręcznie z konsoli albo przy rozsyłce do wszystkich); zmiana karty obsługi idzie na słupki, które ją trzymają. Za każdym razem decyduje najbliższe ustawienie wyżej w drzewie sieci, a przy kartach obsługi drzewo karty, nie słupka.
- Operator w konsoli podgląda słupek: jaką listę kart słupek dostałby teraz, żeby wpuszczać bez pytania centrali? Gdy w sieci nad słupkiem nie włączono listy, widzi pustą.
- Słupek dostaje pełną listę kart (operator wysyła ją z konsoli albo lista idzie na wszystkie słupki naraz): które karty obsługi i które karty gości akceptowanych wydawców słupek ma pamiętać, skoro mieści tylko tyle, ile pozwala jego typ? Karty gości wypadają pierwsze.
- Zmieniła się karta osoby z obsługi: które słupki trzymają ją na swojej liście i muszą dostać zmianę? Decyduje ustawienie listy nad kartą (jej miejscem w sieci), a nie nad słupkiem; gdy nad kartą nie włączono kart obsługi na listach, żaden słupek jej nie dostaje.
- Czy w tym miejscu sieci słupki mają pamiętać listę kart i co ma na niej być? Rozstrzyga najbliższe ustawienie wyżej w drzewie: przy podglądzie i wysyłce listy na słupek, przy zmianie karty obsługi i na ekranie ustawień w konsoli.

Pytanie 8 (Q15): Czy karty tego wydawcy wolno wymieniać z tym partnerem — wysyłać mu je i przyjmować od niego?
Podsumowanie: Dwa przypadki w dwóch kierunkach: przy wysyłce listy kart sieci do partnera decyzja zapada osobno dla każdej karty (karta pominięta, ale i tak uznana za wysłaną), a przy przyjmowaniu kart od partnera — dla całego wydawcy partnera (odrzucona każda linia). W obu rozstrzyga najbliższe ustawienie w górę drzewa wydawców.
- Przy wymianie z partnerami (cyklicznie albo na żądanie operatora) sieć wysyła partnerowi listę swoich kart: czy kartę tego gościa wolno pokazać temu partnerowi? Jeśli nie, partner jej nie dostaje i jego słupki jej nie znają.
- Partner przysyła zmiany w swoich kartach: czy od tego wydawcy w ogóle przyjmujemy karty? Jeśli nie, żadna jego karta nie zostaje dodana, zmieniona ani usunięta, więc jego goście nie ładują na nowych kartach.
- Wspólna reguła obu kierunków wymiany: dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy. Sama nie zna przypadku — ten opisują wysyłka i przyjmowanie kart.

Pytanie 9 (Q16): Czy to karta sieci, czy karta wydana przez partnera — i czy przez tego partnera, który ją teraz przysyła?
Podsumowanie: Jedno pytanie zadawane wyłącznie przy nocnej (albo ręcznie uruchomionej przez administratora) wymianie list kart z partnerem-wydawcą: zanim karta z listy partnera zostanie przyjęta, zmieniona albo wyłączona, system sprawdza, czy to nie karta sieci i czy należy do tego wydawcy, który ją przysyła. Wszystkie pozostałe wiersze to kroki tej samej odpowiedzi, bez osobnych pytających.
- Co noc, albo na żądanie administratora, partner-wydawca przysyła zmiany swojej listy kart: dla każdej karty — czy to nie karta sieci i czy należy do tego partnera, a nie do innego wydawcy? Jeśli nie, ta jedna karta zostaje odrzucona, a pozostałe karty z listy przechodzą dalej.
- Partner przysyła dodanie, zmianę albo wyłączenie karty, którą już znamy jako kartę wydawcy: czy to jego karta, czy innego wydawcy? Jeśli innego, zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy.
- Partner przysyła kartę, którą sieć już ma: czy to karta wydana przez sieć? Jeśli tak, partner nigdy jej nie przejmie, niezależnie od tego, co chce z nią zrobić.
- Partner przysyła kartę: czy znamy ją już jako kartę jakiegoś wydawcy (wtedy sprawdzamy, czy tego partnera), czy jest dla nas nowa? Przy dodaniu nowa karta zostaje założona dla partnera; przy zmianie albo wyłączeniu nieznana karta jest odrzucona jako nieznaleziona.
- Które karty są kartami wydanymi przez sieć? Potrzebne tylko po to, by przy wymianie z partnerem odróżnić karty sieci od kart wydawców.

Pytanie 10 (Q20): Jak jest zorganizowana sieć, za którą odpowiadam: które kempingi należą do których regionów i rynków (a wydawcy i zespoły — do kogo)?
Podsumowanie: Jedno pytanie, jeden przypadek: personel w konsoli (zakładka organizacji) ogląda drzewo sieci w swoim zakresie — kempingi, wydawców albo zespoły. Wszyscy wołający to kolejne piętra tej samej ścieżki pokazującej drzewo; nikt nie podejmuje na nim decyzji.
- Osoba z personelu otwiera w konsoli zakładkę organizacji: jak wygląda sieć w jej zakresie — kempingi w regionach i rynkach (albo wydawcy, albo zespoły)? Widzi drzewo od swojego miejsca w dół.


### Kontekst biznesowy: sieć kempingów sprzedająca prąd na stanowiskach

Plik opisuje, czym ten system jest jako biznes, językiem domeny. Nie zawiera celu analizy ani kandydatów. Każde zdanie ma źródło. Wersja z 2026-10-25 uzupełniona o zapis rozmowy zespołu, zgłoszenia i wiki; wcześniejsza treść zachowana.

#### Legenda źródeł

| Klucz | Źródło | Jak traktować |
|---|---|---|
| `[T: Grzesiek, PO]` | zapis rozmowy 10.03.2026; Grzesiek, product owner po stronie operatora sieci | fakt o intencji biznesu i o tym, co boli |
| `[T: Marta, PO]` | ta sama rozmowa; Marta, product owner po stronie operatora | fakt o intencji biznesu i o tym, co boli |
| `[T: Darek, dev]`, `[T: Kamil, dev]` | ta sama rozmowa; deweloperzy systemu | obserwacja techniczna; diagnoza to hipoteza |
| `[Z: nr, ID]` | zgłoszenia GitHub z narracją użytkownika (402 zgłoszeń, z czego 102 opowiada sytuację); zgłaszający podany jako rola | opis sytuacji lub prośba; nie dowód, że system tak działa |
| `[W: Glosariusz]`, `[W: Autoryzacja]` | strony wiki pisane przez back office dla wsparcia | słowa systemu („słowa konsoli i kodu”) i opis z pamięci back office |
| `[R]` | README repozytorium, napisany przez programistów | opis systemu słowami systemu |
| `[C]` | tematy commitów napisane jako historyjki użytkownika („As a … I can …”), 2012–2017 | deklaracja zamiaru spisana przez programistę; hipoteza o intencji |
| `[K]` | etykiety zakładek i ról konsoli operacyjnej | słowa systemu, które widzi personel |

#### Co robi ten biznes i dla kogo

Operator prowadzi sieć kempingów, głównie w Chorwacji, trochę we Włoszech i Francji, a ostatnio doszła Hiszpania `[T: Grzesiek, PO]`. Sieć sprzedaje energię elektryczną gościom na stanowiskach kempingowych `[R]`. Sieć ciągle kupuje nowe kempingi albo dogaduje się z kimś, i wtedy trzeba je dopisać do struktury, a istniejące regiony przestają pasować, więc kempingi się przesuwa `[T: Grzesiek, PO]`. Włochy są obecnie największym rynkiem pod względem liczby kempingów `[T: Marta, PO]`. System „generalnie działa, goście się ładują, kasa spływa”, ale każda mała prośba realizuje się po kwartale i coś obok się sypie `[T: Grzesiek, PO]`.

Na stanowiskach stoją słupki z gniazdami z licznikiem; gość podłącza się kartą kempingową, kartą domową kempingu albo płatnością QR w aplikacji `[R]`. Sieć wystawia gościom rachunki przez system ERP, wymienia karty i rekordy z partnerskimi wystawcami kart, pilnuje słupków i budżetu mocy sekcji kempingu `[R]`.

#### Strony i czego chcą

- **Gość** chce włączyć i wyłączyć prąd przy swoim gnieździe `[C: CAMP-73]`. Na dużych kempingach, zwłaszcza przy marinie, w szczycie wieczornym czeka 3–4 sekundy, myśli, że karta nie zadziałała, przykłada drugi i trzeci raz i robi się korek `[T: Grzesiek, PO][Z: #294, CAMP-222]`. Gość przejazdowy chce zapłacić przy gnieździe kodem QR i wcześniej zobaczyć szacunek ceny `[C: CAMP-97][Z: #28, QRP-231]`. Zdarza się, że płatność przeszła, a gniazdo nie ruszyło; zwrot robi się ręcznie, gość jest wściekły i pisze recenzję `[T: Grzesiek, PO][Z: #286, QRP-239]`. Gość może być zameldowany na pobyt; wymeldowanie zamyka jego dostawy `[C: PED-50, OPS-34]`. Gość, który zgubił kartę, zgłasza to w recepcji `[T: Grzesiek, PO]`.
- **Recepcja** chce meldować i wymeldowywać gości, szukać ich, blokować karty i zmieniać ich ważność, wydawać kartę powitalną z puli `[C]`. Chce blokady zgubionej karty od razu, jednym kliknięciem, bo dziś zablokowana karta działa jeszcze parę minut i ktoś ładuje na cudzy rachunek `[T: Grzesiek, PO][Z: #48, CRD-375; #161, CRD-369]`. Dla recepcji to ważniejsze niż raporty `[T: Grzesiek, PO]`. Przy meldowaniu robi „sprawdzenie na sucho” karty bez ładowania i na dużych kempingach ono też jest wolne, przez co rośnie kolejka `[T: Grzesiek, PO][Z: #73, CAMP-223]`.
- **Gospodarz kempingu** (kierownik) chce widzieć kluczowe liczby i alerty, obciążenie sekcji, zużycie na godzinę i dzień tygodnia, eksportować raporty, przełączać tryb dostępu `[C: CAMP-141, CAMP-15]`. Chce, by dostawy były odrzucane, gdy budżet sekcji jest pełny `[C][Z: #6, CAMP-314]` – choć to zgłoszenie zamknięto bez naprawy, a inna notatka mówi, że limit jest „doradczy” `[Z: #402, OPS-37]`.
- **Właściciel kempingu / afiliant** chce zarządzać kartami domowymi swojego kempingu i chce, by karta powitalna była przyjmowana przy pierwszym użyciu `[C: CRD-22, CAMP-44]`.
- **Technik serwisu** chce, by milczące słupki były oznaczane, chce maila o błędzie gniazda `[C: OPS-21]` i chce zdalnie zresetować bezpiecznik zamiast jechać na miejsce `[Z: #217, PED-308]`. Chce ostrzeżenia, zanim sekcja się zapełni, nie dopiero po odmowie `[Z: #398, OPS-274]`.
- **Back office** zatwierdza lub odrzuca podejrzane rekordy zużycia (w szczycie kolejka rośnie i chce zatwierdzać zbiorczo) `[C: OPS-34][Z: #255, CRD-353]`, buduje taryfy i symuluje ceny, sprzedaje pakiety sezonowe, ustawia filtry białej listy, uruchamia rozliczenie ERP `[C: QRP-10, PED-48]`. Jedna osoba z back office „ogarnia Chorwację” i po przeniesieniu kempingów do nowego regionu przestała widzieć ich rekordy, choć nikt nie ruszał jej uprawnień `[T: Grzesiek, PO][Z: #3, CAMP-203]`. Dwie osoby ustawione na ten sam rynek widzą różne sumy w tym samym raporcie `[T: Marta, PO][Z: #318, RPT-212]`.
- **Administrator rynku** tworzy grupy w drzewie organizacji i przeszukuje ślad audytu `[C: CAMP-13]`. Po jesiennej przebudowie włoskich regionów administrator włoskiego rynku widział tylko część kempingów i stało przez to fakturowanie `[T: Grzesiek, PO][Z: #347, OPS-202]`.
- **Operacje** porządkują rosnącą sieć: zakładają regiony, przesuwają kempingi, zmieniają nazwy; chcą, by to było bezpieczne, bo dziś „każdy raz to rozbrajanie miny” `[T: Grzesiek, PO]`. Chcą, by małe rzeczy wchodziły w tydzień, nie w kwartał `[T: Grzesiek, PO]`.
- **Księgowość** regularnie zgłasza, że linia rozliczeniowa w ERP pojawia się dwa razy dla tego samego rekordu po korekcie, a starej nikt nie wycofuje; księgowa stornuje ręcznie `[T: Marta, PO][Z: #39, BIL-256]`. Chce numeru stanowiska na linii, by dopasować ją do pobytu `[Z: #317, BIL-250]`. Dzień rozliczeniowy raz w miesiącu „wszystko stoi, bo eksport mieli pół dnia”; biznes chce rozliczenia na bieżąco, ale integracja z ERP jest plikowa i partner po drugiej stronie nie chce jej ruszać `[T: Grzesiek, PO][T: Darek, dev]`.
- **Wystawca kart / partner-wystawca** wystawia i blokuje karty, co noc chce znać stan synchronizacji `[C: CAMP-122][Z: #59, ISS-232]`. Gdy partner podpisuje nowy kemping, jego goście mają od razu móc tam ładować, a dziś trzeba „podłubać w kilku miejscach” (5–6 kroków), zawsze któryś się gubi i karty działają tylko w jedną stronę `[T: Marta, PO][Z: #367, ISS-205; #79, ISS-203]`. Pliki od partnera bywają zapisane z nagłówkiem, który import odrzuca `[Z: #181, ISS-231]`.
- **Pracownicy i kontrahenci kempingu** potrzebują prawa do ładowania na wyjątkach: sezonowy pracownik tylko przy słupkach 3–7 na marinie `[T: Grzesiek, PO][Z: #110, CAMP-210]`; dozorca mieszkający na kempingu zawsze i wszędzie, ale tylko u siebie, nie u sąsiada z tego samego regionu `[T: Grzesiek, PO][Z: #337, CAMP-211]`; ekipy remontowe jedną kartą na zmienny co miesiąc zestaw kempingów `[T: Grzesiek, PO][Z: #345, CRD-201]`. Dziś jedyna możliwość to karta obsługi na cały kemping, co jest „o wiele za dużo” `[T: Grzesiek, PO]`.
- **Sąsiednie kempingi** chcą się dzielić słupkami: Camping Zadar i sąsiad na tydzień festiwalu, z rozliczeniem każdego gościa do jego kempingu macierzystego `[T: Marta, PO][Z: #399, CAMP-215]`; trzy kempingi nad Gardą chcą wspólnego ładowania, ale osobnych linii rozliczeniowych i raportów `[T: Marta, PO][Z: #72, CAMP-218]`. Nikt nie umie powiedzieć, jak to zrobić: przez umowy akceptacji, wspólny region czy inaczej `[T: Marta, PO]`.
- **Słupek** (sterownik) pyta sieć, czy karta może pobierać prąd, chce listy lokalnej kart mieszczącej się w jego pamięci `[C: CRD-22, CAMP-73]`. Około 15% słupków to starsza generacja, która po zaniku prądu wstaje z zegarem na roku 1970 `[T: Kamil, dev]`. Wymiana to sprzęt i nie decydują o tym deweloperzy `[T: Kamil, dev]`.
- **Deweloperzy** wdrażają raz w miesiącu, testy trwają prawie czterdzieści minut, drobna zmiana czeka na okno wdrożeniowe `[T: Darek, dev]`. Zmiana przy grupach albo kartach „wychodzi w raportach albo w rozliczeniu”, testów za mało `[T: Kamil, dev]`. Plan podziału na mniejsze serwisy (raporty, rozliczenia, autoryzacja) istnieje od dwóch lat, ale „nie bardzo wiadomo, gdzie ciąć” `[T: Kamil, dev]`.

#### Co jest sprzedawane i kto się z kim rozlicza

Sprzedawana jest energia pobrana w sesji dostawy; z sesji powstaje wyceniony rekord zużycia `[R]`. Wycena opiera się na taryfach z ograniczeniami czasu, dnia i kWh oraz na sezonowych umowach z pakietami mocy `[R]`. Rachunki dla gości sieci idą przez ERP; miesięcznie powstają linie ERP na konto rozliczeniowe `[R][C: PED-63]`. Rynki (Chorwacja, Włochy, Francja, Hiszpania) to kraje, „po których raportujemy i rozliczamy” `[T: Grzesiek, PO][W: Glosariusz]`. Przy dzieleniu słupków między sąsiadami rozliczenie ma iść „do kempingu macierzystego gościa” `[T: Marta, PO]`. Sezonowa opłata stała poniżej minimalnej kwoty ma być fakturowana `[C: ISS-20]`. Zatrzymanie dostawy kartą domową jest księgowane jako niefakturowane `[C: SUP-8]`. Rekordy importowane od partnera zachowują koszt partnera `[C: BIL-25]`. Rekord może być „podejrzany” i czeka na przegląd `[C: BIL-22]`. Ostatnia kilowatogodzina przed progiem taryfy bywała liczona w złym przedziale `[Z: #15, BIL-253]`. Operacje chcą dopłaty weekendowej w taryfie nad Gardą i na Riwierze `[Z: #346, BIL-267; #142, BIL-272]`.

#### Codzienne sytuacje

- Gość przykłada kartę; słupek pyta sieć; odpowiedź zawiera nazwisko gościa i datę ważności `[C: CRD-22]`. Według back office sprawdzenia idą po kolei i pierwsze, które nie przejdzie, wygrywa: ważność i akceptacja na kempingu, potem karta domowa (przechodzi od razu, omija budżet i zajętość), potem zajętość gniazda, na końcu budżet mocy sekcji `[W: Autoryzacja][T: Darek, dev]`. Kolejność w kodzie nie została tym opisem potwierdzona (zob. „Nie wiadomo”).
- W maju autoryzacja wszędzie śmiga, w sierpniu duże kempingi robią się widocznie wolniejsze na tym samym sprzęcie `[T: Grzesiek, PO][Z: #128, PED-203]`. Autoryzacja jest też ślamazarna dzień–dwa po dużej reorganizacji kempingów, „pewnie bez związku” `[T: Grzesiek, PO][Z: #381, OPS-208]`.
- Kilka razy w tygodniu karta gościa jest odrzucana bez żadnego z normalnych powodów, a wsparcie nie umie powiedzieć dlaczego; częściej w tygodniach, gdy biznes dużo przesuwa w konsoli `[T: Kamil, dev][Z: #334, CAMP-212]`.
- Operacje przenoszą trzy kempingi (Zadar, Split, Rovinj) pod nowy region Dalmacja, żeby się łatwiej raportowało; back office od Chorwacji przestaje widzieć Zadar; deweloperzy „ustawiają przypisanie jeszcze raz” `[T: Grzesiek, PO][T: Kamil, dev][Z: #3, CAMP-203]`. Przeniesienie to technicznie „jedno pole w bazie”, w konsoli „się przeciąga” `[T: Darek, dev]`.
- Ktoś z operacji zmienia nazwę regionu „North” na „HR North”, żeby było konsekwentnie; część kempingów w raportach wychodzi „jako żaden rynek”; cofnięcie nazwy naprawia `[T: Grzesiek, PO][Z: #344, CAMP-204]`. Literówka „HR-North” wysłała linie ERP na domyślny rynek `[Z: #2, CAMP-226]`. Hiszpania chce regionu nazwanego po hiszpańsku, deweloperzy odradzają, bo „rozliczenie się rozjedzie” `[T: Grzesiek, PO][Z: #1, CAMP-229]`. Deweloperzy mają listę rzeczy, których się w konsoli nie rusza; nazwy regionów tam są `[T: Darek, dev]`. Przy migracji danych skrypt poprawił nazwy i kilka regionów „wisiało bez rynku” `[T: Darek, dev][Z: #291, MIG-203]`.
- Po przeniesieniu kempingu do innego regionu zmienia się, kto z obsługi ma prawo tam ładować, „raz mają, raz nie mają”; operacje chcą reguły albo ostrzeżenia przed przeniesieniem `[T: Grzesiek, PO][Z: #336, OPS-205]`.
- Kemping przygarnia zostawioną kartę powitalną jako swoją kartę domową; po przeniesieniu kempingu do nowego regionu karta przestaje autoryzować; ponowne przygarnięcie pomaga `[T: Grzesiek, PO][Z: #193, CAMP-233; #401, CAMP-84]`.
- Operacje ustawiają domyślną taryfę na regionie; część nowych kempingów ją bierze, część nie `[T: Marta, PO][Z: #231, CAMP-232]`. Filtr białej listy czasem znika po zapisie grupy; zgłoszenie zamknięto jako nieodtwarzalne `[T: Marta, PO][Z: #198, CRD-202]`. Gdy filtr odrzuca kartę, komunikat nie mówi, który z kilku filtrów (kemping, region) to zrobił `[T: Marta, PO][Z: #364, ISS-207]`.
- Operator wycofuje stary region; z zeszłego sezonu wraca reklamacja gościa i nikt nie może otworzyć potrzebnych rekordów bez przywrócenia regionu `[T: Grzesiek, PO][Z: #76, CAMP-207]`.
- Raport utylizacji sieci dla Włoch kończy się przekroczeniem czasu, dla małych rynków działa; rok temu działał, gdy Włoch było mniej `[T: Marta, PO][Z: #168, RPT-203]`. Zestawienie zbiorcze po rynkach „każdego sezonu robi się wolniejsze”, choć nikt nie zmienił w nim ani linijki `[T: Marta, PO][Z: #179, RPT-208]`. Raport filtrowany po wysokim regionie „mieli”, po jednym kempingu jest szybki `[Z: #212, RPT-211]`. Raport rozliczeniowy dla dużego wystawcy ciągnie się minutami `[T: Marta, PO][Z: #80, RPT-205]`. Deweloperzy: raporty idą po tej samej bazie, co autoryzacja, i w sezonie „wszystko zwalnia”; chcą repliki lub hurtowni, na co nie ma budżetu `[T: Kamil, dev][T: Darek, dev]`.
- Po burzy część słupków wstaje z rokiem 1970; sesje i rekordy mają kosmiczną datę, ERP je odrzuca, ktoś poprawia ręcznie `[T: Kamil, dev][Z: #57, PED-316]`.
- Kilka razy na sezon rekord ma zero kilowatogodzin, choć kamper „chodził całą noc”; zgłaszane wielokrotnie, bo ludzie nie widzą istniejącego zgłoszenia; hipoteza deweloperów: sesje zamykane automatycznie przy wyjeździe bez końcowego odczytu `[T: Kamil, dev][Z: #21, CRD-378]`.
- Pod koniec miesiąca back office uruchamia rozliczenie ERP `[C]`; korekty rekordów (zera, rok 1970) owocują podwójnymi liniami `[T: Marta, PO]`.
- Co noc wymienia się pliki z partnerami-wystawcami `[C: CAMP-122, ISS-6]`.
- Słupek milczy; technik chce to widzieć `[C: OPS-21]`. Słupek starszej generacji po restarcie przestaje wysyłać sygnał życia, aż ktoś ręcznie poprosi o status `[Z: #226, PED-301]`.
- Nowy słupek przychodzi od dostawcy i jest uruchamiany; serwis chce przypisać gniazdo do stanowiska jednym krokiem z listy magazynowej `[C: PED-48][Z: #35, PED-303]`.

#### Słowa domeny

| Słowo | Rodzaj | Źródło | Uwagi |
|---|---|---|---|
| kemping, stanowisko | business | `[T][R][C]` | miejsce, gdzie stoi gość |
| region, rynek (Chorwacja, Włochy, Francja, Hiszpania) | business | `[T: Grzesiek, PO]` | region to grupa kempingów „żeby się łatwiej raportowało”; rynek to kraj, po którym się raportuje i rozlicza; glosariusz dodaje skróty HR/IT/FR/ES jako słowa systemu |
| porządkowanie / reorganizacja sieci, przenoszenie kempingów | business | `[T: Grzesiek, PO]` | codzienna czynność operacji przy wzroście sieci |
| słupek, gniazdo | system | `[W: Glosariusz][R]` | „trzy generacje łącza” to słowo systemu; biznes mówi „starsze słupki” |
| karta kempingowa | business | `[T][C]` | wystawiana przez wystawcę, ma gościa, ważność, może być blokowana |
| karta domowa | business | `[T: Grzesiek, PO][W: Glosariusz]` | karta kempingu dla obsługi, „ma nie patrzeć na budżet mocy ani zajętość” `[T: Darek, dev]`; glosariusz: omija część sprawdzeń |
| karta obsługi (na cały kemping) | business | `[T: Grzesiek, PO]` | jedyna dostępna forma dla pracowników; „o wiele za dużo” |
| karta powitalna z puli, przygarnięcie karty | business | `[T: Grzesiek, PO][Z: #401]` | karta zostawiona przez gościa staje się kartą domową kempingu |
| sprawdzenie na sucho (dry check) | business | `[T: Grzesiek, PO][Z: #73]` | sprawdzenie karty bez ładowania przy meldowaniu |
| płatność QR, gość przejazdowy | business | `[T: Grzesiek, PO][C: CAMP-97]` | płatność bez karty przez aplikację; „to nowe” |
| sesja dostawy / sesja poboru | system | `[R][W: Glosariusz]` | biznes mówi „ładowanie”, „gość się ładuje” |
| rekord zużycia | system | `[W: Glosariusz][K]` | biznes używa tego słowa też w rozmowie (back office „nie widzi rekordów zużycia”) |
| taryfa, taryfa domyślna na regionie, pakiet mocy, umowa sezonowa | business | `[T: Marta, PO][R][C: QRP-10]` | ograniczenia czasu, dnia, kWh |
| budżet mocy sekcji, „sekcja wywala” | business | `[T][W: Glosariusz]` | glosariusz: nie ma bezpiecznika, to sprawdzenie; limit „doradczy” `[Z: #402]` |
| wystawca kart (issuer), partner-wystawca, umowa akceptacji | business | `[T: Marta, PO][R]` | umowa, że karty wystawcy działają na danych kempingach |
| kemping macierzysty gościa | business | `[T: Marta, PO]` | do niego ma iść rozliczenie przy dzieleniu słupków |
| whitelista / filtr białej listy, lista lokalna | business? | `[T: Marta, PO][C]` | PO używa słowa „whitelisty”; lista lokalna to słowo systemu (karty w pamięci słupka) |
| rozliczenie ERP, linia rozliczeniowa, storno, dzień rozliczeniowy, konto rozliczeniowe | business | `[T: Marta, PO][T: Grzesiek, PO][R]` | miesięczne, przez zewnętrzny ERP; storno robi księgowa ręcznie |
| raport utylizacji sieci, zestawienie zbiorcze po rynkach, raport rozliczeniowy wystawcy | business | `[T: Marta, PO]` | nazwy raportów w ustach PO; w systemie nie potwierdzone pod tymi nazwami |
| drzewa SITE / ISSUER / STAFF, grupa, tryb dostępu | system | `[W: Glosariusz][R][C: CAMP-15]` | biznes mówi „struktura w konsoli jak foldery” `[T: Darek, dev]` |
| pobyt, meldunek, wymeldowanie | business | `[C]` | |
| incydent dostawy | system | `[C: CAMP-141]` | |
| „aktywny” | system | `[W: Autoryzacja]` | wiki ostrzega, że znaczy co innego w autoryzacji niż w raportach i eksporcie |

#### Sprzeczności między źródłami

- **Kolejność sprawdzeń przy autoryzacji.** Wiki i Darek mówią: ważność → karta domowa → zajętość → budżet `[W: Autoryzacja][T: Darek, dev]`. Kod tej kolejności nie potwierdza (zob. „Nie wiadomo”).
- **Częstość rozsyłania list kart.** Kamil: „jobem co kilka minut” `[T: Kamil, dev]`; wartość domyślna w systemie jest inna – nie potwierdzono, co jest ustawione w produkcji.
- **Różne sumy raportu.** Kamil podejrzewa cache na kilkanaście minut `[T: Kamil, dev]`; zgłaszający sądzi, że zależy od miejsca w drzewie `[Z: #318, RPT-212]`.
- **Odrzucanie przy pełnej sekcji.** Kierownik chce odrzucania `[C][Z: #6]`; notatka OPS-37 mówi, że limit jest doradczy i karty domowe go omijają `[Z: #402]`; glosariusz powtarza „limit jest doradczy” `[W: Glosariusz]`.
- **Dezaktywacja regionu.** PO i zgłoszenie mówią o „wycofaniu/zdezaktywowaniu” regionu `[T: Grzesiek, PO][Z: #76]`; w systemie taka operacja nie została potwierdzona.
- **Przenoszenie kempingu „przeciąganiem w konsoli”** `[T: Darek, dev]` – operacja przeniesienia nie została potwierdzona w interfejsie konsoli.

#### Nie wiadomo

- **Kto płaci komu.** Czy gość płaci kempingowi, sieci czy wystawcy; jak partner-wystawca rozlicza się z siecią; co dokładnie znaczy „rozliczenie do kempingu macierzystego” `[T: Marta, PO]` i „rozliczany przez kemping” `[C]`.
- **Czym jest rynek biznesowo** (spółka, kraj, waluta, podatki?) poza tym, że „po nim raportujemy i rozliczamy” `[T: Grzesiek, PO]`; dlaczego nazwa regionu ma nieść rynek – deweloperzy to wiedzą, biznes nie `[T]`.
- **Czym różnią się role**: afiliant/właściciel, operator kempingu, gospodarz, kierownik; kto jest „obsługą” z kartą domową, a kto „pracownikiem sezonowym” `[T][R][C]`.
- **Co znaczy tryb dostępu kempingu** i kiedy właściciel go zmienia `[C: CAMP-15]`.
- **Jak miałaby działać wymiana gości między sąsiadami** – przez umowy akceptacji, wspólny region czy nowy mechanizm; nikt nie sprawdził, co się stanie `[T: Marta, PO][T: Darek, dev]`.
- **Jakie są te 5–6 kroków podłączenia kempingu do umowy z wystawcą** – instrukcja ma być na wiki, w publicznym wiki jej nie ma `[T: Marta, PO]`.
- **Ile naprawdę trwa autoryzacja, jak często zdarzają się odmowy bez powodu i zera kWh** – wszystkie liczby to wspomnienia z rozmowy, bez logów `[T]`.
- **Kto funduje karty powitalne** i co biznesowo znaczy „przygarnięcie” `[Z: #401][C: CAMP-44]`.
- **Jakie funkcje są planowane** – cel mówi „nowe funkcje mają wchodzić szybciej”; z rozmowy wynika lista życzeń (bezpieczne porządki, wyjątki w prawie do ładowania, dzielenie słupków, blokada od razu, rozliczenie na bieżąco), ale bez priorytetów `[T]`.
- **Czy daty historii są prawdziwe**: commity 2012–2017, zgłoszenia datowane 2012–2017, a rozmowa z 2026 mówi o tych samych problemach jako bieżących `[C][Z][T]`.
- **Czy identyfikatory zgłoszeń i commitów to ten sam system** – numeracja się nie pokrywa `[Z][C]`.


## Raport

### Wynik

Jedno wystąpienie jednego archetypu: **Party**. Pozostałe osiem archetypów (Product, Pricing, Rules & Scoring, Accounting, Inventory, Ordering, Plan vs Wykonanie, Matching) nie pasuje. Żaden agent nie zgłosił werdyktu NIEROZSTRZYGNIĘTE, więc FAZA 2 (pytania do użytkownika) została pominięta.

### Wystąpienia

| wystąpienie (język biznesu) | archetyp | kluczowy dowód (cytat) | zastrzeżenia |
|---|---|---|---|
| Wydawcy kart (sieć i partnerzy-wystawcy) z organizacjami nadrzędnymi, kempingi (własne, afiliowane, niezależne) i personel kempingów, powiązani umowami akceptacji, dziedziczeniem po organizacji nadrzędnej, wymianą z partnerem i przynależnością personelu do kempingu | `party` | „Gość przykłada kartę do gniazda słupka (albo operator z konsoli uruchamia prąd lub sprawdza kartę na gnieździe): czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza.” (PROBLEM, Q1) | Wielorolowości jednego podmiotu wejście nie dowodzi (pojawia się tylko w życzeniach). Kemping jako podmiot potwierdzony tylko częściowo: regiony i rynki to grupy do raportowania, nie podmioty. Relacje są dziś pozycją w trzech drzewach, a kontekst sam stwierdza, że nie wiadomo, czym różnią się role afiliant/właściciel, operator kempingu, gospodarz, kierownik. |

Elementy archetypu Party w tym wystąpieniu (z raportu agenta):

- Podmioty z tożsamością ← wystawca kart / partner-wystawca — „**Wystawca kart / partner-wystawca** wystawia i blokuje karty, co noc chce znać stan synchronizacji” (kontekst, „Strony i czego chcą”)
- Podmioty z tożsamością ← gość jako posiadacz karty — „Gość przykłada kartę; słupek pyta sieć; odpowiedź zawiera nazwisko gościa i datę ważności” (kontekst, „Codzienne sytuacje”)
- Relacje między podmiotami z semantyką ← wydawca ↔ organizacja nadrzędna, dziedziczenie umów — „Operator dodał albo zdjął umowę wydawcy z kempingiem (albo tylko ją podgląda): na których kempingach karty tego wydawcy działają teraz i czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?” (PROBLEM, Q3)
- Relacje między podmiotami z semantyką ← wydawca ↔ partner, zgoda na wymianę kart rozstrzygana po drzewie wydawców — „dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy” (PROBLEM, Q15)
- Relacje między podmiotami z semantyką (uprawnienie z relacji) ← personel ↔ kemping/region — „czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu, nawet przy zajętym gnieździe i wyczerpanej mocy sekcji. Jeśli nie, karta przechodzi zwykłe sprawdzenia gościa.” (PROBLEM, Q4)
- Relacje między podmiotami z semantyką (uprawnienie wobec konkretnej rzeczy z relacji) ← karta należy do wydawcy, tylko on może ją zmieniać — „Partner przysyła dodanie, zmianę albo wyłączenie karty, którą już znamy jako kartę wydawcy: czy to jego karta, czy innego wydawcy? Jeśli innego, zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy.” (PROBLEM, Q16)
- Zdolności podmiotów (kto ma prawo zrobić X w miejscu Y) ← zasięg umów wydawcy — „Operator w konsoli otwiera umowy wydawcy: na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej?” (PROBLEM, Q3)

Elementy bez dowodu: wielorolowość (jeden podmiot w dwóch rolach naraz albo migracja roli); osoby i organizacje wymiennie / duplikacja danych.

### Nie pasują

- **Product** — PROBLEM nie zawiera fragmentu, w którym biznes definiowałby ofertę osobno od egzemplarzy klientów. Odrzucone tropy: „buduje taryfy i symuluje ceny, sprzedaje pakiety sezonowe” to mechanika wyceny (anty-sygnał), poza zakresem paczki pytań; rodzaje kart (kempingowa, domowa, obsługi, powitalna, personelu) to stałe stany sterujące uprawnieniami, nie katalog; przemiana karty powitalnej w kartę personelu to zmiana statusu egzemplarza, nie upgrade między typami; „typ słupka” to typ urządzenia z pojemnością, nie pozycja oferty; umowa akceptacji i tryb dostępu kempingu to relacja i dwustanowy atrybut.
- **Pricing** — w PROBLEMIE pytanie „ile wynosi X dla Y w chwili T” nie pada ani razu; odpowiedzi są binarne („prąd płynie albo nie”) albo są listą. Odrzucone tropy: taryfy z progami czasu, dnia i kWh, dopłata weekendowa, szacunek ceny przy QR — wszystkie z kontekstu, dotyczą wyceny rekordu zużycia i rozliczenia, poza paczką pytań; „karty gości wypadają pierwsze” to priorytet przycinania listy, nie wyliczana wartość; „najbliższe ustawienie wyżej w drzewie” to dziedziczona flaga, nie funkcja wyceny.
- **Rules & Scoring** — kaskada autoryzacji to cztery sprawdzenia o stałym kształcie i kolejności („pierwsze, które nie przejdzie, wygrywa”); nikt spoza zespołu nie dopisuje ani nie zmienia warunków. To, co operatorzy konfigurują (umowy, karty domowe, tryb dostępu, flagi list i wymiany), to dane o relacjach i pojedyncze parametry dziedziczone po drzewie. Odrzucone tropy: życzenia o wyjątkach w prawie do ładowania (pracownik sezonowy przy słupkach 3–7, dozorca „tylko u siebie”, ekipy remontowe) mają jeden kształt „karta → zakres miejsc”, brak kompozycji; „komunikat nie mówi, który z kilku filtrów to zrobił” to jedno ustawienie na różnych piętrach drzewa; „operacje chcą reguły albo ostrzeżenia” to słowo potoczne.
- **Accounting** — cała paczka rozstrzyga stan binarny; nic nie jest liczone, dodawane ani odejmowane. Odrzucone tropy: „pula” kart powitalnych to zbiór z przynależnością, nie saldo; pojemność listy słupka to stały limit bez historii; budżet mocy sekcji to test „czy jeszcze jest miejsce” z limitem „doradczym”; ważność karty to data/flaga; storno i linie ERP są poza paczką pytań i w zewnętrznym ERP.
- **Inventory/Availability** — w żadnym z dziesięciu pytań nie ma skończonego zasobu, o który ubiega się więcej niż jeden aktor i który system przydziela. Odrzucone tropy: budżet mocy sekcji leży poza pytaniami PROBLEMU, występuje tylko jako sprawdzenie omijane przez karty personelu, a kontekst opisuje limit jako „doradczy”; „zajęte gniazdo” to wyłączność fizyczna bez wyścigu; pamięć słupka na listę lokalną to deterministyczne przycięcie listy; „pula” kart powitalnych bez dowodu skończoności; „kolejka” przy słupku i w recepcji to objaw wolnej autoryzacji.
- **Ordering** — decyzja przy słupku jest natychmiastowa: intencja (przyłożenie karty) i realizacja (prąd) w jednym kroku, bez bytu-zlecenia, stanu „przyjęte” i anulowania. Odrzucone tropy: płatność QR z ręcznym zwrotem (odstęp między zapłatą a wykonaniem, ale bez tożsamości zlecenia i stanów, poza zakresem PROBLEMU); sesja dostawy to pomiar poboru; wiersz zmiany od partnera to synchronizacja danych; przegląd podejrzanych rekordów to akceptacja dokumentu; pobyt to rejestracja obecności; przejęcie karty powitalnej to zmiana statusu w tle.
- **Plan vs Wykonanie** — nigdzie nie ma pary „miało być / faktycznie wyszło” z pytaniem „o ile” i politykami interpretacji. Odrzucone tropy: budżet mocy sekcji to limit sprawdzany w chwili autoryzacji; ostrzeżenie „zanim sekcja się zapełni” to próg na bieżącej wartości; pakiety sezonowe to sam plan bez zestawienia z wykorzystaniem; szacunek ceny jest tylko wyświetlany; „rekord podejrzany”, podwójna linia ERP, „płatność przeszła, a gniazdo nie ruszyło” to anomalie i defekty, nie odchylenia od utrwalonego planu.
- **Matching/Optymalizacja** — odpowiedzi wynikają z umów, drzewa i ustawień, czyli z reguł dających jednoznaczny wynik; nikt nie pyta o najlepszy przydział ani „co by było, gdyby”. Odrzucone tropy: pamięć słupka („karty gości wypadają pierwsze”) to stała reguła pierwszeństwa bez miary „lepszej listy”; budżet mocy to kolejka „kto pierwszy”, poza paczką pytań; „dopasować linię do pobytu” to parowanie rekordów, fałszywy przyjaciel; filtrowanie kart przy wymianie z partnerem to przydział jednoznaczny.

### Nierozstrzygnięte

Brak. Żaden agent nie oddał pytań do użytkownika.

### Ten sam fragment w kilku archetypach

Nie dotyczy: jest jedno wystąpienie. Dla porządku: fragment „karty gości wypadają pierwsze” (Q4, Q12) i budżet mocy sekcji były rozważane i odrzucone niezależnie przez Pricing, Accounting, Inventory, Matching i Plan vs Wykonanie; wszyscy odrzucili je z tego samego powodu (stały limit lub stała reguła pierwszeństwa, bez miary, salda, konkurencji ani planu).

## Opowieść

W paczce pytań „Czy ta karta dostanie prąd na tym kempingu?” występuje jeden archetyp: **Party**, w relacjach między wydawcami kart, kempingami, ich organizacjami nadrzędnymi, partnerami i personelem.

**Wydawcy, kempingi i personel w relacjach (Party).** Podmiotami z tożsamością są tu wydawcy kart: sieć i partnerzy-wystawcy („Wystawca kart / partner-wystawca wystawia i blokuje karty, co noc chce znać stan synchronizacji”), oraz gość jako posiadacz karty, którego słupek rozpoznaje po nazwisku i dacie ważności. To, czy w gnieździe popłynie prąd, nie wynika z cechy samej karty, tylko z relacji między podmiotami. Pierwsza relacja to umowa akceptacji wydawcy z kempingiem: „czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza”. Druga to dziedziczenie tej umowy po organizacji nadrzędnej wydawcy: operator chce wiedzieć, „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej”. Trzecia to przynależność personelu do kempingu lub regionu, która daje uprawnienie silniejsze od reszty sprawdzeń: „czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu, nawet przy zajętym gnieździe i wyczerpanej mocy sekcji”. Czwarta to zgoda na wymianę kart z partnerem, rozstrzygana po drzewie wydawców: „sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy”. Piąta to własność karty przez wydawcę: „czy to jego karta, czy innego wydawcy? Jeśli innego, zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy”. Zasięg umów wydawcy („na których kempingach goście z kartami tego wydawcy mogą ładować”) to zdolność podmiotu wynikająca z tych relacji. Zastrzeżenia agenta: wielorolowości jednego podmiotu wejście nie pokazuje w działającej mechanice, a tylko w życzeniach (sąsiednie kempingi naraz gospodarzem i kempingiem macierzystym gościa; pracownik z prawem „tylko u siebie, nie u sąsiada z tego samego regionu”); regiony i rynki to grupy do raportowania, nie podmioty; relacje żyją dziś jako pozycja w trzech drzewach, a kontekst sam przyznaje, że nie wiadomo, czym różnią się role afiliant/właściciel, operator kempingu, gospodarz i kierownik.

**Czego nie ma.** Pozostałe osiem archetypów nie pasuje do tej paczki pytań, bo każde pytanie kończy się odpowiedzią tak/nie albo listą, nigdy kwotą, saldem, przydziałem ani odchyleniem. Taryfy, pakiety sezonowe, linie ERP i storno istnieją w kontekście systemu, ale dotyczą wyceny i rozliczenia zużycia, których paczka pytań nie obejmuje (Pricing, Accounting, Product). Budżet mocy sekcji, zajęte gniazdo i pojemność listy słupka wyglądają na ograniczone zasoby, ale w wejściu nikt o nie nie konkuruje, limit jest „doradczy”, a przycinanie listy to stała reguła pierwszeństwa (Inventory, Matching). Kaskada sprawdzeń na słupku ma stały kształt i stałą kolejność, a to, co operator konfiguruje, to relacje i pojedyncze flagi dziedziczone po drzewie, nie komponowane reguły (Rules). Decyzja przy słupku zapada w jednym kroku, bez zlecenia, stanów i anulowania (Ordering), i nigdzie nie ma zestawienia planu z wykonaniem (Plan vs Wykonanie).

### Integracje

Brak. Jest jedno wystąpienie, a integracje wolno prowadzić tylko od Orderingu, Oferty i Rules; żadne z nich nie pasuje. Oferta nie powstaje, bo wymaga co najmniej dwóch wystąpień PASUJE.

### Odniesienia semantyczne

Brak. Odniesienie wymaga dwóch końców z werdyktem PASUJE, a jest tylko jeden. Wystąpienie jest niezależne; reguły łączenia nie dopuszczają tu żadnego połączenia.

## Wystąpienia od agentów

### party — PASUJE

```
ARCHETYP: party
WERDYKT ZBIORCZY: PASUJE

WYSTĄPIENIA:
1. Wydawcy kart (sieć i partnerzy-wystawcy) z organizacjami nadrzędnymi, kempingi (własne, afiliowane, niezależne) i personel kempingów, powiązani umowami akceptacji, dziedziczeniem po organizacji nadrzędnej, wymianą z partnerem i przynależnością personelu do kempingu
   werdykt: PASUJE
   pytanie główne: TAK — „Gość przykłada kartę do gniazda słupka (albo operator z konsoli uruchamia prąd lub sprawdza kartę na gnieździe): czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza." (_problem-wejscie.md, Q1, linia 10) — logika zależy od relacji wydawca–kemping; o samej wielorolowości jednego podmiotu wejście nie świadczy (zob. elementy bez dowodu)
   elementy:
     - Podmioty z tożsamością ← wystawca kart / partner-wystawca — „**Wystawca kart / partner-wystawca** wystawia i blokuje karty, co noc chce znać stan synchronizacji" (business-context.md, „Strony i czego chcą", linia 35)
     - Podmioty z tożsamością ← gość jako posiadacz karty — „Gość przykłada kartę; słupek pyta sieć; odpowiedź zawiera nazwisko gościa i datę ważności" (business-context.md, „Codzienne sytuacje", linia 47)
     - Relacje między podmiotami z semantyką ← wydawca ↔ organizacja nadrzędna, dziedziczenie umów — „Operator dodał albo zdjął umowę wydawcy z kempingiem (albo tylko ją podgląda): na których kempingach karty tego wydawcy działają teraz i czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?" (_problem-wejscie.md, Q3, linia 19)
     - Relacje między podmiotami z semantyką ← wydawca ↔ partner, zgoda na wymianę kart rozstrzygana po drzewie wydawców — „dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy" (_problem-wejscie.md, Q15, linia 53)
     - Relacje między podmiotami z semantyką (uprawnienie z relacji) ← personel ↔ kemping/region — „czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu, nawet przy zajętym gnieździe i wyczerpanej mocy sekcji. Jeśli nie, karta przechodzi zwykłe sprawdzenia gościa." (_problem-wejscie.md, Q4, linia 23)
     - Relacje między podmiotami z semantyką (uprawnienie wobec konkretnej rzeczy z relacji) ← karta należy do wydawcy, tylko on może ją zmieniać — „Partner przysyła dodanie, zmianę albo wyłączenie karty, którą już znamy jako kartę wydawcy: czy to jego karta, czy innego wydawcy? Jeśli innego, zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy." (_problem-wejscie.md, Q16, linia 58)
     - Zdolności podmiotów (kto ma prawo zrobić X w miejscu Y) ← zasięg umów wydawcy — „Operator w konsoli otwiera umowy wydawcy: na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej?" (_problem-wejscie.md, Q3, linia 18)
   elementy bez dowodu: Wielorolowość (element 2) — wejście nie pokazuje jednego podmiotu w dwóch rolach naraz ani migracji roli; Osoby i organizacje wymiennie / duplikacja danych (element 4) — brak dowodu.
   anty-sygnały: „Każdy podmiot ma jedną stałą rolę, a relacje nie wpływają na logikę" — nie zachodzi: umowa akceptacji, dziedziczenie po organizacji nadrzędnej, przynależność personelu do kempingu i zgoda na wymianę z partnerem wprost decydują, czy prąd rusza i czy karta jest przyjęta/wymieniana. „Mały system z banalną listą klientów" — nie zachodzi: sieć w czterech krajach, „ciągle kupuje nowe kempingi albo dogaduje się z kimś" (business-context.md, linia 20), partnerzy podpisują nowe kempingi w „5–6 kroków, zawsze któryś się gubi" (linia 35).
   zastrzeżenia: (a) Kemping jako podmiot jest w wejściu tylko częściowo potwierdzony — bywa stroną umowy i ma właściciela/afilianta („kemping afiliowany", „niezależny kemping", „Właściciel kempingu / afiliant"), ale regiony i rynki to grupy „żeby się łatwiej raportowało", nie podmioty; dowody elementu 3 oparte na parze wydawca–organizacja nadrzędna i wydawca–partner są od tego niezależne. (b) Relacje są dziś realizowane jako pozycja w trzech drzewach („drzewa SITE / ISSUER / STAFF", „struktura w konsoli jak foldery"), a wejście samo stwierdza, że „Czym różnią się role: afiliant/właściciel, operator kempingu, gospodarz, kierownik" nie wiadomo (business-context.md, „Nie wiadomo", linia 105). (c) Sygnały wielorolowości są wyłącznie życzeniami, nie działającą mechaniką: sąsiednie kempingi chcą być naraz gospodarzem i „kempingiem macierzystym gościa" (linia 37), a pracownicy i kontrahenci chcą praw „tylko u siebie, nie u sąsiada z tego samego regionu" (linia 36) — dziś „jedyna możliwość to karta obsługi na cały kemping". (d) O skali wejście nie milczy — sieć rośnie i jest reorganizowana.

ODRZUCONE TROPY:
- „Gość", „Recepcja", „Gospodarz kempingu", „Technik serwisu", „Back office", „Administrator rynku", „Operacje", „Księgowość" (business-context.md, „Strony i czego chcą", linie 26–39) — lista aktorów i etykiet ról konsoli `[K]`; nazwy aktorów to nie dowód, brak mechaniki, w której jeden z nich byłby naraz innym.
- „Osoba z personelu otwiera w konsoli zakładkę organizacji: jak wygląda sieć w jej zakresie — kempingi w regionach i rynkach" (_problem-wejscie.md, Q20, linia 65) — drzewo pokazywane do oglądania, „nikt nie podejmuje na nim decyzji"; hierarchia użyta do wyświetlenia drzewka to fałszywy przyjaciel.
- „Wydawca spoza jego zakresu albo coś, co nie jest wydawcą: odmowa" (_problem-wejscie.md, Q3, linia 18) oraz back office, które „ogarnia Chorwację" i „przestała widzieć ich rekordy" (business-context.md, linia 31) — zakres widoczności wynikający z miejsca personelu w drzewie; to poziom dostępu do poddrzewa, bliżej uprawnień niż relacji między podmiotami; nie użyte jako dowód.
- „karta gościa staje się kartą personelu kempingu" (_problem-wejscie.md, Q4, linia 26; Q10, linia 33) — migruje rola karty (rzeczy), nie rola osoby czy organizacji; gość kartę zostawił, więc nie jest to ten sam podmiot w nowej roli.
- „Które karty są kartami wydanymi przez sieć?" (_problem-wejscie.md, Q16, linia 61) — sieć występuje jako jeden z wydawców obok partnerów, co mogłoby sugerować dwie role sieci (operator i wydawca), ale wejście używa tego tylko do odróżnienia kart przy imporcie i nie opisuje sieci jako podmiotu modelowanego w dwóch rolach; za słabe na element 2.
- „Sąsiednie kempingi chcą się dzielić słupkami … z rozliczeniem każdego gościa do jego kempingu macierzystego" (business-context.md, linia 37) — kemping jako gospodarz i jako macierzysty naraz byłby wielorolowością, ale to życzenie, o którym „Nikt nie umie powiedzieć, jak to zrobić"; odnotowane w zastrzeżeniach, nie jako dowód.

UZASADNIENIE (2–4 zdania): Pytanie główne ma odpowiedź TAK w drugiej części: to, czy gość dostanie prąd, czy karta zostanie przyjęta i czy zostanie wymieniona z partnerem, zależy wprost od relacji między podmiotami — umowy akceptacji wydawcy z kempingiem, dziedziczenia umów po organizacji nadrzędnej wydawcy, zgody na wymianę rozstrzyganej po drzewie wydawców oraz przynależności personelu do kempingu lub regionu. Element 1 (wydawcy, gość z nazwiskiem i ważnością) oraz elementy 3 i 5 mają własne cytaty, żaden anty-sygnał nie zachodzi, a wejście opisuje rosnącą, stale reorganizowaną sieć, więc test „zwykły kod wystarczy" nie blokuje. Wielorolowości jednego podmiotu wejście nie dowodzi (pojawia się tylko w życzeniach), co obniża pewność, ale nie zmienia werdyktu, bo karta wymaga elementu 1 i co najmniej jednego z 2–5.
```

Odpowiedzi użytkownika z FAZY 2: brak (agent nie zadał pytań).

## Propozycja pogłębienia

Jedyny archetyp z werdyktem PASUJE i mapperem w tej wersji: **`party`**. Jego wystąpienie obejmuje cały rdzeń problemu: umowy wydawca–kemping (Q1–Q3), personel kempingu (Q4), własność kart i wymianę z partnerem (Q15, Q16).

```
/archetype-storyteller Pogłębienie: party. Skan: analiza/archetypy.md. Miejsca: 
```

Po „Miejsca:” dopisz po przecinku miejsca w kodzie (`Klasa.metoda`), pod spodem wklej bolączki z adresami w kodzie (od `# BOLĄCZKI Z ADRESAMI W KODZIE`), dopiero potem Enter. Uruchom w nowej sesji albo po `/clear`. Bez listy miejsc mapper sam wybiera 5 miejsc z najprostszą odpowiedzią; „Miejsca: wszystkie” daje wiersz dla każdego miejsca z bolączek.

## Pogłębienie: party

Uruchomienie: 2026-10-25. Tryb: wskazane miejsca (ValidationService.validateFor, GroupService.findIssuerGroupIdsAcceptedAt, LocalCardListService.buildLocalCardList, WelcomeCardAdoptionService.checkPedestal, IssuerCardImportService.importCard). Dwie rundy: pierwszy raport mappera miał 4 pytania (P1–P4); użytkownik odpowiedział, mapper poprawił model i zwrócił raport KOMPLETNE. Surowe raporty: `analiza/mapper-party-raport-r1.md` (runda 1), `analiza/mapper-party-raport-r2.md` (runda 2, końcowy); bolączki podane mapperowi: `analiza/bolaczki-party.md`.

### Wynik

Jedno wystąpienie archetypu Party zmapowane na poziomie 3 (rzeczywistość dużej organizacji); status KOMPLETNE, bez pytań otwartych i bez sprzeciwu wobec werdyktu.

| wystąpienie | archetyp | poziom złożoności | status mapowania | liczba założeń |
|---|---|---|---|---|
| Wydawcy kart (sieć i partnerzy-wystawcy) z organizacjami nadrzędnymi, kempingi (własne, afiliowane, niezależne) i personel kempingów, powiązani umowami akceptacji, dziedziczeniem po organizacji nadrzędnej, wymianą z partnerem i przynależnością personelu do kempingu | party | 3 | KOMPLETNE | 15 |

#### Odpowiedzi użytkownika na pytania mappera (runda 1)

- **P1 (region jako podmiot czy etykieta):** opcja A, z zastrzeżeniem „nie mam pewności”. Uzasadnienie użytkownika: z bolączek wynika, że biznes chce przenosić kemping do innego regionu tylko dla raportowania, bez zmiany tego, kto na nim ładuje i kto widzi jego rekordy; region raczej jako etykieta raportowa, przeniesienie nie zmienia praw.
- **P2 (druga strona umowy akceptacji):** „nie wiem, zostaw jak dziś” → wariant roboczy A (umowa z kempingiem).
- **P3 (dziedziczenie umów po organizacji nadrzędnej):** „nie wiem, zostaw jak dziś” → opcja A (najbliższy wydawca z własnymi umowami zastępuje wyższe).
- **P4 (zasięg praw personelu):** cały kemping lub region; o zakresach słupków użytkownik nic nie wie → opcja A, bez `Capability`.

### Opowieść i łączenie

Opowieść i łączenie bez zmian: sekcja `## Opowieść` wyżej. Pogłębienie nie zmienia połączeń: jest jedno wystąpienie PASUJE, więc nie ma drugiego końca ani dla integracji, ani dla odniesienia semantycznego. Kandydaci z pola `IDENTYFIKATORY I RZUTY` mappera pozostają kandydatami bez drugiego końca:

- UID karty kempingowej — identyfikator rzeczy (karty), przechowywany jako identyfikator KARTA posiadacza: „karta kempingowa… wystawiana przez wystawcę, ma gościa, ważność, może być blokowana” (analiza/archetypy.md:147)
- Kod słupka → kemping (`PartyId`), rzut potrzebny do każdego pytania „czy tu”: „czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek?” (analiza/archetypy.md:18)
- Numer karty i konto rozliczeniowe z linii importu partnera — rzut rozliczeń przy identyfikatorze KARTA: „billing account number” w `importCard` (analiza/bolaczki-party.md:211; campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:92)
- Kod rynku HR/IT/FR/ES — identyfikator jednostki RYNEK, używany przez rozliczenie i raporty: „rynek to kraj, po którym się raportuje i rozlicza” (analiza/archetypy.md:144)

### Mapowanie

Pole `MODEL` z raportu mappera (runda 2; nagłówki obniżone o dwa poziomy), bez zmian:

### Party Archetype Model: Wydawcy kart, kempingi i personel sieci kempingów

#### Complexity Level

**Level 3 — rzeczywistość dużej organizacji.** Dowody:
- organizacje z własną strukturą wewnętrzną: sieć w czterech krajach, „ciągle kupuje nowe kempingi albo dogaduje się z kimś, i wtedy trzeba je dopisać do struktury” (analiza/archetypy.md:95); „drzewa SITE / ISSUER / STAFF” (analiza/archetypy.md:162);
- wydawcy z organizacjami nadrzędnymi: „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej” (analiza/archetypy.md:27);
- partnerzy jako kontrahenci plikowi: „Co noc wymienia się pliki z partnerami-wystawcami” (analiza/archetypy.md:135);
- relacje decydują o logice: „czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza.” (analiza/archetypy.md:18); „czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu” (analiza/archetypy.md:31).

**In scope:** Person, Company, OrganizationUnit; jeden `PartyId`; `RegisteredIdentifier` (karta kempingowa, kod rynku); role jako katalog `PartyRoleType`; `PartyRelationship` jako autonomiczne jednostki z regułami i ważnością; uprawnienia wyprowadzane z relacji (karta uprawnień); pytania sieciowe (ścieżki po drzewie wydawców).
**Out of scope:** `Capability` i `OperatingScope` (prawa ładowania personelu obejmują zawsze cały kemping — odpowiedź P4); adresy; wymagania ról (`RoleRequirements`); podtypy prawne `Company` (wejście nie różnicuje formy prawnej).

#### Concept Mapping

| Domain Concept | Party Archetype Element | Notes |
|---|---|---|
| Sieć / operator sieci | `Company` + rola globalna OPERATOR SIECI + rola globalna WYDAWCA | Jeden podmiot w dwu rolach: „Operator prowadzi sieć kempingów” (analiza/archetypy.md:95) i „Które karty są kartami wydanymi przez sieć?” (analiza/archetypy.md:69) |
| Wystawca kart / wydawca | `Company` (lub `OrganizationUnit`, gdy to jednostka organizacji nadrzędnej) + rola globalna WYDAWCA | „wystawiana przez wystawcę” (analiza/archetypy.md:147) |
| Partner-wystawca | WYDAWCA + strona `to` relacji WYMIANA KART | Partner = wydawca, z którym sieć wymienia karty (analiza/archetypy.md:59–60) |
| Organizacja nadrzędna wydawcy / „wyżej w drzewie wydawców” | relacja PODLEGŁOŚĆ WYDAWCY | „na najbliższym wyżej w drzewie wydawców” (analiza/archetypy.md:61) |
| Kemping (własny, afiliowany, niezależny) | `OrganizationUnit` + rola globalna KEMPING | Jedyna strona umów akceptacji i przynależności personelu; „kemping afiliowany”, „niezależny kemping” (analiza/archetypy.md:38, 43) |
| Region, rynek | `OrganizationUnit` + rola globalna REGION / RYNEK; relacja PRZYNALEŻNOŚĆ W SIECI | Etykieta raportowa (odpowiedź P1): „region to grupa kempingów «żeby się łatwiej raportowało»; rynek to kraj” (analiza/archetypy.md:144). Z przynależności nie wynika żadne prawo ładowania |
| Właściciel kempingu / afiliant | `Company` + strona `from` relacji PROWADZENIE KEMPINGU | „Właściciel kempingu / afiliant chce zarządzać kartami domowymi swojego kempingu” (analiza/archetypy.md:104) |
| Umowa akceptacji | relacja UMOWA AKCEPTACJI (Wydawca → Kemping) | „umowa, że karty wystawcy działają na danych kempingach” (analiza/archetypy.md:157); druga strona: kemping (odpowiedź P2: „jak dziś”) |
| Umowa „na region” | zbiór relacji UMOWA AKCEPTACJI z każdym kempingiem regionu w chwili zawarcia | Założenie (zob. ZAŁOŻENIA); „kempingi/regiony wpisane w umowę” (analiza/bolaczki-party.md:26) |
| Umowa własna vs odziedziczona | ścieżka wyprowadzenia uprawnienia: wprost vs przez PODLEGŁOŚĆ WYDAWCY; najbliższy wydawca z własnymi umowami zastępuje wyższe | analiza/archetypy.md:27 (odpowiedź P3: „jak dziś”) |
| Gość (posiadacz karty) | `Person` + strona `to` relacji OBSŁUGA KART WYDAWCY | „odpowiedź zawiera nazwisko gościa i datę ważności” (analiza/archetypy.md:122) |
| Karta kempingowa (UID, ważność, blokada) | `RegisteredIdentifier` typu KARTA z `Validity` | „ma gościa, ważność, może być blokowana” (analiza/archetypy.md:147); rola karty wynika z posiadacza, nie z flagi |
| Karta domowa / karta obsługi / karta personelu | ten sam identyfikator KARTA zarejestrowany na osobie z personelu albo na kempingu/właścicielu | „karta kempingu dla obsługi” (analiza/archetypy.md:148); dziś bez osoby: „karta obsługi na cały kemping” (analiza/archetypy.md:149) |
| Karta personelu „na region” | relacje PERSONEL z każdym kempingiem regionu w chwili wydania | „karta personelu tego kempingu lub jego regionu” (analiza/archetypy.md:31); założenie (zob. ZAŁOŻENIA) |
| Karta powitalna z puli | identyfikator KARTA zarejestrowany na jednostce PULA KART POWITALNYCH rynku | „karta powitalna z puli” (analiza/archetypy.md:150) |
| Przygarnięcie karty powitalnej | przerejestrowanie identyfikatora z puli na kemping/właściciela (zdarzenie) | „karta zostawiona przez gościa staje się kartą domową kempingu” (analiza/archetypy.md:150) |
| Pula kart powitalnych rynku | `OrganizationUnit` sieci-wydawcy, po jednej na rynek | „pula kraju słupka” (analiza/archetypy.md:38) |
| Personel kempingu (obsługa, dozorca, sezonowy, ekipa) | relacja PERSONEL (Kemping → Person) z rolą z katalogu | „dozorca… tylko u siebie” (analiza/archetypy.md:111) |
| Back office, administrator rynku, operator kempingu | relacja PERSONEL z rolą z katalogu; dla ról raportowych drugą stroną może być REGION/RYNEK | „Jedna osoba z back office «ogarnia Chorwację»” (analiza/archetypy.md:106); czym różnią się role: nie wiadomo (analiza/archetypy.md:180) |
| „Karty wydawcy działają na kempingu” | uprawnienie AKCEPTACJA KART (Wydawca → Kemping), z relacji | analiza/archetypy.md:18 |
| „Karta personelu ładuje bez ograniczeń” | uprawnienie ŁADOWANIE SŁUŻBOWE (posiadacz → Kemping), z relacji PERSONEL | analiza/archetypy.md:31 |
| „Czy z tym partnerem wymieniamy” (wysyłka, przyjmowanie) | relacja WYMIANA KART w dwu kierunkach + uprawnienia UDOSTĘPNIANIE KART / PRZYJMOWANIE KART | analiza/archetypy.md:59–61 |
| „Czy to jego karta, czy innego wydawcy” | uprawnienie ZMIANA KART (Wydawca → Posiadacz), z relacji OBSŁUGA KART WYDAWCY | analiza/archetypy.md:66 |
| „Otwarty na karty sieci” / „tylko własne karty obsługi” | obecność/brak relacji UMOWA AKCEPTACJI sieci-jako-wydawcy z tym kempingiem (wariant roboczy) | analiza/archetypy.md:43–48; alternatywa: rola globalna kempingu (zob. ZAŁOŻENIA) |
| Kod rynku HR/IT/FR/ES | `RegisteredIdentifier` typu KOD RYNKU na jednostce RYNEK | „glosariusz dodaje skróty HR/IT/FR/ES” (analiza/archetypy.md:144) |
| Przeniesienie kempingu do innego regionu | zakończenie jednej relacji PRZYNALEŻNOŚĆ W SIECI i rozpoczęcie drugiej; umowy i personel kempingu nietknięte | „Przeniesienie to technicznie «jedno pole w bazie»” (analiza/archetypy.md:125); cel: „bez zmiany tego, kto na nim ładuje i kto widzi jego rekordy” (analiza/bolaczki-party.md:6) |
| Zakres widoczności personelu w konsoli | uprawnienie WGLĄD (osoba → jednostka i jej członkowie), z relacji PERSONEL | „Wydawca spoza jego zakresu albo coś, co nie jest wydawcą: odmowa” (analiza/archetypy.md:26) |

#### Unmapped Concepts

- Słupek, gniazdo, kod słupka, sekcja — rzeczy, nie podmioty; model potrzebuje tylko rzutu „słupek → kemping” (zob. IDENTYFIKATORY I RZUTY). Prawa węższe niż kemping (słupki 3–7) nie są modelowane (odpowiedź P4).
- Lista lokalna słupka: czy słupek ma ją trzymać, co na niej, pojemność sterownika, kolejność ucinania — ustawienie i mechanika urządzenia; archetyp daje tylko skład „kto ma uprawnienie”, nie listę.
- Zgoda na przejmowanie kart powitalnych (ustawienie dziedziczone po strukturze) — ustawienie jednostki, nie relacja ani rola.
- Decyzja o prądzie: zajętość gniazda, budżet mocy sekcji, płatność QR, sesja dostawy — proces, który pyta model.
- Aktywacja karty, numer karty, konto rozliczeniowe, koszt partnera — cechy karty i rozliczeń poza tożsamością posiadacza.
- Flagi słupków przy trybie dostępu (`listedInApp`, `remoteControl`, `settledByCampsite`), taryfa domyślna regionu, filtry białej listy jako mechanizm.
- Widoczność rekordów zużycia w raportach, rozliczenie z kempingiem macierzystym — model daje uprawnienie WGLĄD jako fakt o dwu podmiotach; co z nim zrobić, decyduje raport/rozliczenie.
- Drzewo konsoli (Q20) — widok struktury; zapytanie po relacjach, bez własnego elementu.
- Wymiana plików z partnerem jako proces (cykl nocny, statusy kierunków, odrzucenia linii).

#### Party Types

| Party Type | Represents | Own Data | Notes |
|---|---|---|---|
| `Company` | sieć (operator), partnerzy-wystawcy, organizacje nadrzędne wydawców, właściciele/afilianci kempingów | nazwa | Ta sama firma może być naraz wydawcą i właścicielem kempingu — jedno `PartyId` |
| `OrganizationUnit` | kempingi; regiony i rynki (jednostki raportowe); jednostki drzewa wydawcy (pula kart powitalnych rynku, podjednostki kart sieci) | nazwa, nazwa wyświetlana | Rodzaj jednostki (kemping / region / rynek / pula) to rola, nie typ |
| `Person` | gość (posiadacz karty), osoba z personelu (obsługa, dozorca, sezonowy, back office, administrator rynku) | nazwisko (imię) | „nazwisko gościa” (analiza/archetypy.md:122); dziś personel nie ma osób, tylko karty (zob. Implementation Notes) |

**PartyId:** losowy UUID generowany w aplikacji; jedna przestrzeń dla wszystkich typów. UID karty, kod rynku ani nazwa grupy nigdy nie są kluczem (dziś tryb dostępu i kemping afiliowany wynikają z nazwy grupy — analiza/bolaczki-party.md:138, 143).

#### Registered Identifiers

| Identifier | Value / Validation | Validity | Assignable To |
|---|---|---|---|
| KARTA (UID karty kempingowej) | UID RFID (hex/dec, normalizowany) | od–do; blokada = skrócenie ważności do „teraz”; nieważna karta nie identyfikuje posiadacza przy słupku („ważność i akceptacja na kempingu” — analiza/archetypy.md:122) | `Person` (gość, osoba z personelu); `OrganizationUnit` KEMPING lub `Company` właściciel (karta obsługi bez osoby, przygarnięta); `OrganizationUnit` PULA KART POWITALNYCH (karta nieprzydzielona) |
| KOD RYNKU | HR / IT / FR / ES | brak (nie wygasa) | tylko `OrganizationUnit` w roli RYNEK |

**Assignment policy:** jedna klasa polityki (reguły zależą od typu i roli podmiotu: karta tylko na osobie, kempingu, właścicielu lub puli; kod rynku tylko na rynku), egzekwowana w `Party.add(identifier, policy)`; dostarczana stałą konfiguracją. Przerejestrowanie identyfikatora KARTA z puli na kemping (przygarnięcie) i z puli na gościa (wydanie karty powitalnej w recepcji) przechodzi przez tę samą politykę.

#### Roles

**Strategy:** 3 — `PartyRoleType` (katalog). Powód: lista ról personelu jest nieustalona („Czym różnią się role: afiliant/właściciel, operator kempingu, gospodarz, kierownik; kto jest «obsługą»…” — analiza/archetypy.md:180), biznes nazywa nowe rodzaje personelu („sezonowy pracownik”, „dozorca”, „ekipy remontowe” — analiza/archetypy.md:111), dochodzą nowe rynki; katalog pozwala dopisać rolę bez wdrożenia (wdrożenia raz w miesiącu — analiza/archetypy.md:114). Decyzja (Z).

| Role | Form | Eligible Party Types / Constraints | Held By (domain) |
|---|---|---|---|
| OPERATOR SIECI | globalna | `Company`; dokładnie jeden podmiot | sieć |
| WYDAWCA | globalna; także strona `from` UMOWY AKCEPTACJI i OBSŁUGI KART WYDAWCY | `Company`, `OrganizationUnit` | sieć, partnerzy, organizacje nadrzędne, jednostki drzewa wydawcy |
| ORGANIZACJA NADRZĘDNA / WYDAWCA PODLEGŁY | w relacji PODLEGŁOŚĆ WYDAWCY (from / to) | obie strony w roli WYDAWCA | drzewo wydawców |
| PARTNER-WYSTAWCA | w relacji WYMIANA KART (to) | `Company` w roli WYDAWCA, inny niż sieć | partnerzy, z którymi wymienia się pliki |
| KEMPING | globalna | `OrganizationUnit` | kempingi własne, afiliowane, niezależne |
| REGION, RYNEK | globalna | `OrganizationUnit`; RYNEK ma identyfikator KOD RYNKU; nie są stroną UMOWY AKCEPTACJI ani PERSONELU ładującego | regiony, kraje — jednostki raportowe |
| PULA KART POWITALNYCH | globalna | `OrganizationUnit` pod sieciom-wydawcą; co najwyżej jedna na rynek | pule kart powitalnych |
| WŁAŚCICIEL / AFILIANT | w relacji PROWADZENIE KEMPINGU (from) | `Company` | sieć (kempingi własne), afiliant, właściciel niezależnego |
| MIEJSCE AKCEPTACJI | w relacji UMOWA AKCEPTACJI (to) | tylko `OrganizationUnit` w roli KEMPING | kempingi „wpisane w umowę” |
| POSIADACZ KART WYDAWCY (GOŚĆ) | w relacji OBSŁUGA KART WYDAWCY (to) | `Person`; wyjątkowo `Company` partnera jako posiadacz zastępczy (zob. ZAŁOŻENIA) | goście sieci i partnerów |
| PERSONEL ŁADUJĄCY: OBSŁUGA, DOZORCA, PRACOWNIK SEZONOWY, EKIPA REMONTOWA | w relacji PERSONEL (to); from = KEMPING | tylko `Person` | personel kempingów |
| PERSONEL RAPORTOWY: BACK OFFICE, ADMINISTRATOR RYNKU, OPERATOR KEMPINGU, GOSPODARZ, TECHNIK | w relacji PERSONEL (to); from = KEMPING, REGION lub RYNEK | tylko `Person`; daje WGLĄD, nie ŁADOWANIE SŁUŻBOWE | personel konsoli; różnice ról nieznane (analiza/archetypy.md:180) |

#### Party Core vs Orbiting Data

**Atomic core:** `PartyId`, zarejestrowane identyfikatory (KARTA, KOD RYNKU), role, `version` (blokada optymistyczna).

| Data | Decision | Reason (gate questions) |
|---|---|---|
| Identyfikatory KARTA (UID, ważność, blokada) | w rdzeniu | Współtworzą tożsamość przy słupku; zmiana musi być natychmiast spójna („blokady zgubionej karty od razu… dziś zablokowana karta działa jeszcze parę minut” — analiza/archetypy.md:102) |
| Relacje (umowy, podległość, personel, przynależność, wymiana) | osobne jednostki `PartyRelationship` | Wiele relacji na podmiot, reguły patrzą na role obu stron, zmieniane przez różne osoby |
| Zgoda na przejmowanie kart powitalnych, ustawienie listy lokalnej, taryfa domyślna, flagi słupków | poza rdzeniem (ustawienia jednostki keyed by `PartyId`, poza tym modelem) | Nie współtworzą tożsamości; własne reguły dziedziczenia; zmieniane niezależnie |
| Nazwa wyświetlana jednostki | poza rdzeniem | Zmiana nazwy nie może zmieniać uprawnień („zmiana nazwy regionu… część kempingów wychodzi «jako żaden rynek»” — analiza/archetypy.md:126) |
| Dane rozliczeniowe karty (konto, koszt partnera) | poza modelem | Nie o tożsamości |

Adresy: nie w zakresie.

#### Relationships

**Consistency boundary:** autonomiczne `PartyRelationship` — relacji jest wiele (każdy wydawca × kemping, każdy pracownik × kemping), reguły patrzą na role obu stron, a dostępność autoryzacji ważniejsza niż ścisła spójność. Naruszenia kwalitatywne zapobiegane przy tworzeniu; ilościowe (jeden rodzic, jeden wydawca posiadacza, jeden właściciel) zapobiegane, bo dotyczą jednej strony.

| Relationship | From (role, party types) | To (role, party types) | Validity | Rules (quantitative / qualitative / temporal) | Violation Handling |
|---|---|---|---|---|---|
| PODLEGŁOŚĆ WYDAWCY | ORGANIZACJA NADRZĘDNA (Company/Unit w roli WYDAWCA) | WYDAWCA PODLEGŁY (Company/Unit w roli WYDAWCA) | otwarta, z historią | jeden rodzic naraz, bez cykli / obie strony WYDAWCA / brak | prevented |
| UMOWA AKCEPTACJI | WYDAWCA (Company/Unit) | MIEJSCE AKCEPTACJI (Unit w roli KEMPING) | od–do, dziś otwarta; (Z) data startu i końca dozwolone | brak limitu / from musi mieć WYDAWCA, to musi mieć KEMPING / brak | prevented |
| PRZYNALEŻNOŚĆ W SIECI | JEDNOSTKA NADRZĘDNA (Unit RYNEK lub REGION) | CZŁONEK (Unit REGION lub KEMPING) | od–do, z historią („będziemy dalej reorganizować” — analiza/bolaczki-party.md:7) | jeden rodzic naraz, bez cykli / rynek ← region ← kemping / brak. **Nie wyprowadza żadnego prawa ładowania** | prevented |
| PROWADZENIE KEMPINGU | WŁAŚCICIEL / AFILIANT (Company) | KEMPING (Unit) | otwarta | dokładnie jeden właściciel kempingu / brak / brak | prevented |
| PERSONEL | KEMPING (Unit); dla ról raportowych także REGION/RYNEK | PERSONEL z rolą z katalogu (Person) | od–do (sezon) | brak / tylko Person po stronie to; rola ładująca tylko z KEMPINGIEM / brak | prevented |
| OBSŁUGA KART WYDAWCY | WYDAWCA (Company/Unit, w tym PULA) | POSIADACZ (Person; posiadacz zastępczy partnera) | otwarta | **dokładnie jeden wydawca na posiadacza** („karta zostaje u dotychczasowego wydawcy” — analiza/archetypy.md:66) / from musi mieć WYDAWCA / brak | prevented |
| WYMIANA KART — kierunek WYSYŁKA | JEDNOSTKA SIECI-WYDAWCY (Company sieć lub jej Unit) | PARTNER-WYSTAWCA (Company) | otwarta | brak / to musi mieć WYDAWCA i nie być siecią / brak | prevented |
| WYMIANA KART — kierunek PRZYJMOWANIE | PARTNER-WYSTAWCA (Company lub jego Unit) | SIEĆ (Company) | otwarta | brak / jw. / brak | prevented |

Skutki krzyżowe: zakończenie PRZYNALEŻNOŚCI W SIECI (przeniesienie kempingu) nie dotyka UMOWY AKCEPTACJI ani PERSONELU — obie zawierane są wyłącznie z kempingiem, więc po przeniesieniu ładują ci sami. Zmienia się tylko zakres WGLĄDU ról raportowych związanych ze starym i nowym regionem. Zakończenie OBSŁUGI KART WYDAWCY wymaga wcześniejszego zdjęcia identyfikatorów KARTA posiadacza albo przeniesienia ich na innego wydawcę (dziś: „partner nigdy jej nie przejmie” — analiza/archetypy.md:67).

#### Permissions in the Context of Other Parties

| Permission | Holder → Context Party | Source (relationship / path / direct grant) | Validity | Used by Decision (+ other data) |
|---|---|---|---|---|
| AKCEPTACJA KART | Wydawca → Kemping | wprost: UMOWA AKCEPTACJI; albo PODLEGŁOŚĆ WYDAWCY → UMOWA AKCEPTACJI najbliższej organizacji nadrzędnej, która ma jakiekolwiek umowy (własne umowy zastępują wyższe — „jak dziś”, P3); nadanie bezpośrednie: nie | z relacji | autoryzacja przy słupku (Q1), lista lokalna (Q2), ekran umów (Q3); plus: ważność identyfikatora KARTA, rzut słupek→kemping, zajętość, budżet |
| ŁADOWANIE SŁUŻBOWE | Posiadacz karty (Person z personelu albo Kemping/Właściciel jako posiadacz) → Kemping | PERSONEL z tym kempingiem (tylko wprost; karta „na region” to wiele takich relacji); dla przygarniętej karty: posiadacz = kemping/właściciel, więc wobec siebie (PROWADZENIE KEMPINGU) | z relacji | autoryzacja (Q4): omija umowę, ważność, zajętość i budżet — ta decyzja poza modelem |
| UDOSTĘPNIANIE KART | Jednostka sieci-wydawcy → Partner | WYMIANA KART (wysyłka) na jednostce albo najbliższej nad nią (PODLEGŁOŚĆ WYDAWCY); brak → polityka domyślna (dziś: wolno — analiza/bolaczki-party.md:194) | z relacji | eksport kart do partnera (Q15); plus: które karty czekają na eksport |
| PRZYJMOWANIE KART | Partner → Sieć | WYMIANA KART (przyjmowanie) na partnerze albo najbliższej organizacji nad nim; brak → polityka domyślna (dziś: wolno) | z relacji | import listy partnera (Q15) |
| ZMIANA KART | Wydawca → Posiadacz | OBSŁUGA KART WYDAWCY wprost albo przez PODLEGŁOŚĆ WYDAWCY (posiadacze wydawców podległych); posiadacze sieci: nikt poza siecią | z relacji | import linii partnera (Q16); plus: czy UID znany, kod akcji |
| WGLĄD / ZARZĄDZANIE | Person z personelu raportowego → Jednostka i jej aktualni członkowie (PRZYNALEŻNOŚĆ W SIECI) / podlegli (PODLEGŁOŚĆ WYDAWCY) | PERSONEL z rolą BACK OFFICE / ADMINISTRATOR RYNKU / OPERATOR KEMPINGU | z relacji | zakres konsoli (Q3 „wydawca spoza jego zakresu”, Q20); co widać w raportach — poza modelem. Jedyne uprawnienie, które idzie po przynależności, bo do tego regiony służą (założenie) |

Karta uprawnień: dla wydawcy — wszystkie kempingi, na których jego karty są przyjmowane, każdy ze źródłem (własna umowa / umowa organizacji X) i ważnością; dla kempingu — wszyscy wydawcy i cały personel z prawem ładowania, ze źródłem.

#### Capabilities

Not in scope. Prawo ładowania personelu obejmuje zawsze cały kemping (odpowiedź P4); o zakresach słupków biznes nic nie wie, więc `Capability` z `LocationScope` czeka, aż taki wymóg się pojawi (wtedy: typ ŁADOWANIE SŁUŻBOWE Z ZAKRESEM, zakresy miejsca i czasu, trzymany przez `Person` z relacją PERSONEL).

#### Questions the Model Answers

| Question | Answered By |
|---|---|
| Q1: czy wydawca karty ma umowę obejmującą kemping słupka? | identyfikator KARTA → posiadacz → OBSŁUGA KART WYDAWCY → uprawnienie AKCEPTACJA KART wobec kempingu (wprost / po organizacji nadrzędnej) |
| Q2: karty których wydawców przyjmuje kemping? | wszyscy wydawcy z uprawnieniem AKCEPTACJA KART wobec kempingu (pytanie sieciowe odwrotne) |
| Q3: na których kempingach działają karty wydawcy i czy umowa własna, czy odziedziczona? | karta uprawnień wydawcy: lista kempingów ze źródłem każdej pozycji |
| Q4: czy to karta personelu kempingu (lub wydana „na region”, który go wtedy obejmował)? | identyfikator KARTA → posiadacz → uprawnienie ŁADOWANIE SŁUŻBOWE wobec kempingu z relacji PERSONEL z tym kempingiem |
| Q10/Q11: czy karta jest z puli kraju kempingu, czy kemping jest afiliowany i przyjmuje tylko własne karty? | posiadacz identyfikatora = PULA rynku (rynek kempingu odczytany po PRZYNALEŻNOŚCI); PROWADZENIE KEMPINGU (właściciel ≠ sieć); brak UMOWY AKCEPTACJI sieci-wydawcy z kempingiem |
| Q12: które karty na listę słupka? | składniki: posiadacze z ŁADOWANIEM SŁUŻBOWYM + posiadacze wydawców z AKCEPTACJĄ KART; reszta (czy lista, pojemność) poza modelem |
| Q15: czy z tym partnerem wymieniamy karty? | UDOSTĘPNIANIE KART / PRZYJMOWANIE KART z relacji WYMIANA KART po drzewie wydawców |
| Q16: czy to karta sieci, czy tego partnera? | wydawca posiadacza; uprawnienie ZMIANA KART |
| Q20: jak zorganizowana jest sieć w moim zakresie? | PRZYNALEŻNOŚĆ W SIECI / PODLEGŁOŚĆ WYDAWCY / PERSONEL od jednostki z relacji WGLĄD w dół |
| B1: co się zmieni, gdy przeniosę kemping do innego regionu? | dla ładowania: nic (żadne prawo ładowania nie idzie po PRZYNALEŻNOŚCI); dla konsoli: lista osób z WGLĄDEM przez stary i nowy region (pytanie sieciowe) |
| B3: gdzie działa ta karta i dlaczego? | karta uprawnień posiadacza/wydawcy ze źródłem każdej pozycji |

#### Implementation Notes

- Kemping, region, rynek, wydawca, pula to **podmioty z własnym `PartyId`**, a nie węzły jednego drzewa; „miejsce w drzewie” rozpada się na trzy niezależne relacje (PRZYNALEŻNOŚĆ W SIECI, PODLEGŁOŚĆ WYDAWCY, PERSONEL). Przeniesienie kempingu kończy jedną PRZYNALEŻNOŚĆ i zaczyna drugą; umowy i personel kempingu nie są pozycją w drzewie, więc nie zmieniają się po cichu — to wprost realizuje cel B1.
- Region jest etykietą raportową (P1): umowa „na region” i karta personelu „na region” to **wachlarz relacji z kempingami należącymi do regionu w chwili zawarcia/wydania**. Konsola może oferować „zawrzyj z całym regionem” jako skrót tworzący wiele relacji; kemping dopisany do regionu później nie dostaje ich automatycznie, kemping przeniesiony — nie traci. Użytkownik tej decyzji nie jest pewny (zob. ZAŁOŻENIA).
- Funkcja karty (gościa / obsługi / powitalna) **nie jest flagą**: wynika z tego, kto jest dziś posiadaczem identyfikatora i jakie ten posiadacz ma relacje. Przygarnięcie = przerejestrowanie identyfikatora z puli na kemping/właściciela; wydanie karty powitalnej w recepcji = przerejestrowanie z puli na gościa.
- Polityki dostarczane z zewnątrz: (a) polityka wyprowadzania AKCEPTACJI KART po PODLEGŁOŚCI WYDAWCY — przyjęta „najbliższy wydawca z własnymi umowami zastępuje wyższe” (jak dziś, P3), wymienialna bez zmiany sprawdzenia; (b) polityka domyślna wymiany przy braku relacji WYMIANA KART (dziś: wolno); (c) polityka przydziału identyfikatora KARTA; (d) polityka ról w relacji PERSONEL (tylko Person; rola ładująca tylko z kempingiem). Sprawdzenie jest stałe, polityka wymienna.
- Zdarzenia: start/koniec UMOWY AKCEPTACJI, PERSONELU, PRZYNALEŻNOŚCI, zmiana identyfikatora KARTA — na nie reaguje odświeżenie list lokalnych słupków (dziś zdarzenia `HOUSE_CARD_CHANGED`, `AccessModeSwitchedEvent`; dla umów brak zdarzenia — stąd „podłubać w kilku miejscach”, analiza/archetypy.md:110).
- Słupek trzyma własną kopię (listę lokalną) jako rzut na chwilę; model nie odpowiada za jej aktualność.
- Dziś personel nie ma osób, tylko karty. Wariant roboczy: karta obsługi bez osoby wisi jako identyfikator na kempingu (lub właścicielu); gdy biznes zacznie rejestrować osoby, identyfikator przechodzi na Person z relacją PERSONEL — bez zmiany reguł.
- Karty importowane od partnera nie mają znanej osoby: identyfikator rejestrowany na posiadaczu zastępczym partnera (jeden na partnera, jak dziś `getPartnerGuest`), z relacją OBSŁUGA KART WYDAWCY do partnera.
- Nazwa grupy nie może nieść znaczenia (dziś: „AFFILIATE …”, „NETWORK ACCESS <MKT>”, „HOUSE CARDS ONLY <MKT>”, „NETWORK CARD”, nazwa puli) — zastępują ją rola, identyfikator KOD RYNKU i relacje.

Implementacja referencyjna: Brak referencyjnej implementacji w paczce skilli — implementacja powstanie od zera według tej specyfikacji.

#### Decyzje mappera

- Sieć to jeden podmiot (`Company`) w dwu rolach: OPERATOR SIECI i WYDAWCA — (W) „Operator prowadzi sieć kempingów” (analiza/archetypy.md:95); „Które karty są kartami wydanymi przez sieć?” (analiza/archetypy.md:69)
- Wydawca, partner, organizacja nadrzędna to role i relacje jednego typu podmiotu, nie osobne typy — (W) „dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców” (analiza/archetypy.md:61)
- Kemping jest podmiotem (`OrganizationUnit`), stroną UMOWY AKCEPTACJI, PERSONELU i PROWADZENIA — (W) „umowę wydawcy z kempingiem” (analiza/archetypy.md:19); „czy to karta personelu tego kempingu” (analiza/archetypy.md:31); „Właściciel kempingu / afiliant” (analiza/archetypy.md:104)
- Region i rynek są jednostkami raportowymi; z PRZYNALEŻNOŚCI W SIECI nie wynika żadne prawo ładowania; UMOWA AKCEPTACJI i PERSONEL ładujący zawierane tylko z kempingiem — (Z) przyjęte po odpowiedzi użytkownika (P1, opcja A, „nie mam pewności”): „kemping raportowany w regionie, w którym biznes chce go widzieć, bez zmiany tego, kto na nim ładuje i kto widzi jego rekordy” (analiza/bolaczki-party.md:6); „region to grupa kempingów «żeby się łatwiej raportowało»” (analiza/archetypy.md:144)
- Umowa i karta personelu „na region” = wachlarz relacji z kempingami regionu w chwili zawarcia/wydania — (Z) wariant przyjęty, wynika z P1-A; alternatywy w ZAŁOŻENIACH
- Druga strona UMOWY AKCEPTACJI: kemping, nie sieć jako całość — (Z) „jak dziś” (odpowiedź P2); wejście: „umowę wydawcy z kempingiem” (analiza/archetypy.md:19)
- Dziedziczenie umów po PODLEGŁOŚCI WYDAWCY: najbliższy wydawca z własnymi umowami zastępuje wyższe — (Z) „jak dziś” (odpowiedź P3); wejście: „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej” (analiza/archetypy.md:27)
- Zasięg praw personelu: cały kemping, bez zakresów słupków; `Capability` poza zakresem — (Z) przyjęte (odpowiedź P4, opcja A)
- Posiadacz kart ma dokładnie jednego wydawcę (reguła ilościowa OBSŁUGI KART WYDAWCY) — (W) „zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy” (analiza/archetypy.md:66); „partner nigdy jej nie przejmie” (analiza/archetypy.md:67)
- Karta kempingowa to zarejestrowany identyfikator posiadacza z ważnością, nie osobny podmiot ani flaga typu — (W) „ma gościa, ważność, może być blokowana” (analiza/archetypy.md:147); „odpowiedź zawiera nazwisko gościa i datę ważności” (analiza/archetypy.md:122)
- Przygarnięcie = przerejestrowanie identyfikatora z puli na kemping/właściciela — (W) „karta gościa staje się kartą personelu kempingu” (analiza/archetypy.md:34); „karta staje się kartą obsługi właściciela kempingu i znika z puli” (analiza/archetypy.md:41)
- Wymiana z partnerem w dwu kierunkach jako dwie relacje asymetryczne, rozstrzygane po drzewie wydawców — (W) „Dwa przypadki w dwóch kierunkach… W obu rozstrzyga najbliższe ustawienie w górę drzewa wydawców” (analiza/archetypy.md:58)
- Brak ustawienia wymiany = wolno (polityka domyślna poza modelem) — (W) „brak filtra = tak” (analiza/bolaczki-party.md:194, 199)
- Tryb „otwarty na karty sieci / tylko własne karty” = obecność/brak UMOWY AKCEPTACJI sieci-wydawcy z kempingiem — (Z) wariant roboczy; alternatywa: rola globalna kempingu (zob. ZAŁOŻENIA)
- Strategia ról: katalog `PartyRoleType` — (Z) wariant roboczy: 3, bo role personelu nieustalone (analiza/archetypy.md:180)
- Relacje jako autonomiczne jednostki, naruszenia zapobiegane — (Z) wariant roboczy, mały wpływ
- Zakres widoczności w konsoli jako uprawnienie WGLĄD z relacji PERSONEL, idące po PRZYNALEŻNOŚCI W SIECI (jedyne takie) — (W) „Wydawca spoza jego zakresu albo coś, co nie jest wydawcą: odmowa” (analiza/archetypy.md:26); „przestała widzieć ich rekordy, choć nikt nie ruszał jej uprawnień” (analiza/archetypy.md:106); (Z) że po przynależności — bo rynki to kraje, „po których raportujemy i rozliczamy” (analiza/archetypy.md:118)

#### Założenia mappera

- **P1 przyjęte z zastrzeżeniem użytkownika „nie mam pewności”**: region jako etykieta raportowa. Jeśli biznes jednak chce, by prawa „na region” podążały za kempingami przenoszonymi między regionami, model zmienia się tylko w ścieżkach uprawnień AKCEPTACJA KART i ŁADOWANIE SŁUŻBOWE (dochodzi krok przez PRZYNALEŻNOŚĆ W SIECI) oraz w roli MIEJSCE AKCEPTACJI (dopuszcza REGION); reszta zostaje.
- Karta personelu „na region” w wariancie A: w chwili wydania powstaje po jednej relacji PERSONEL z każdym kempingiem, który wtedy należy do regionu (ta sama ważność, ta sama rola); kemping dopisany do regionu później relacji nie dostaje, kemping przeniesiony — nie traci. Alternatywa odrzucona: relacja PERSONEL z regionem i wyprowadzenie po przynależności (to wariant B z P1). Konsola pokazuje te relacje jako jedną „kartę regionu”, jeśli chce.
- Umowa akceptacji „na region” analogicznie: wachlarz relacji UMOWA AKCEPTACJI z kempingami regionu w chwili zawarcia; dodanie kempingu do regionu nie rozszerza umowy samo.
- WGLĄD ról raportowych (back office, administrator rynku) jako jedyne uprawnienie idzie po PRZYNALEŻNOŚCI W SIECI — raportowanie jest tym, do czego regiony i rynki służą; przeniesienie kempingu w obrębie tego samego rynku nie zmienia widoczności osób związanych z rynkiem, zmienia tylko dla osób związanych ze starym/nowym regionem.
- P2 „zostaw jak dziś”: UMOWA AKCEPTACJI zawierana z kempingiem; dodaje ją operator sieci (dziś) — kto jeszcze może ją dodać lub wypowiedzieć (właściciel/afiliant?), poza modelem do czasu decyzji.
- P3 „jak dziś”: polityka zastępowania; zmiana na sumę to wymiana polityki, nie modelu.
- P4: prawa ładowania personelu zawsze na cały kemping; prośby „słupki 3–7” i „zestaw kempingów zmieniany co miesiąc” obsługuje się odpowiednio: nie da się / wiele relacji PERSONEL z ważnością miesięczną.
- `PartyId` = losowy UUID; nazwy grup przestają nieść znaczenie (prefiksy „AFFILIATE”, „NETWORK ACCESS <MKT>”, „NETWORK CARD”).
- Karty importowane od partnera bez znanej osoby wiszą na jednym posiadaczu zastępczym partnera (jak dziś `getPartnerGuest`); gdy partner zacznie przysyłać osoby, identyfikatory przechodzą na `Person`.
- Karta obsługi bez osoby jest identyfikatorem zarejestrowanym na kempingu (lub właścicielu), a jej prawo ładowania wynika z tego, że posiadacz jest tym kempingiem; alternatywa do wprowadzenia bez zmiany reguł: osoba z relacją PERSONEL.
- Tryb dostępu kempingu niezależnego zamodelowany jako umowa akceptacji sieci-wydawcy z kempingiem; jeśli biznes woli trzymać go jako cechę kempingu, to rola globalna OTWARTY NA KARTY SIECI / TYLKO WŁASNE KARTY o tym samym skutku w pytaniach Q10/Q11.
- Zgoda na przejmowanie kart powitalnych i ustawienie listy lokalnej to ustawienia jednostki poza modelem; dziedziczą po PRZYNALEŻNOŚCI W SIECI tak jak dziś po drzewie (to ustawienia, nie prawa ładowania, więc nie łamią P1-A).
- Pula kart powitalnych to jednostka sieci-wydawcy, najwyżej jedna na rynek; rynek karty ustala się po posiadaczu (pula), rynek kempingu odczytuje się po PRZYNALEŻNOŚCI W SIECI (odczyt etykiety, nie uprawnienie).
- Umowy akceptacji dostają ważność od–do (dziś otwarte); nie wpływa to na dzisiejsze zachowanie, dopóki wszystkie są otwarte.
- Rola OPERATOR SIECI ma dokładnie jednego posiadacza (jeden operator w systemie).

#### Co i jak

| CO | JAK STARE | JAK W ARCHETYPIE |
|---|---|---|
| **ValidationService.validateFor (Q1):** Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony. | `campsite-core/src/main/java/io/pillopl/card/ValidationService.java:261` — łańcuch pierwszy-błąd: słupek istnieje → karta personelu → QR → karta znana → (gdy dostawa trwa: tylko czy jest gość) → `isCardAcceptedAt(card, pedestal)` = `groupService.isCardAcceptedAt(pedestalId, card)`: karta bez gościa → false; `extractAllAcceptingSiteGroupIds(guest.group)` (najbliższa w górę drzewa wydawców niepusta lista umów SITE, całe poddrzewa) ∩ grupa słupka; false → `CARD_NOT_ACCEPTED_AT_SITE`. Zwraca `boolean` — tak: wydawca karty ma umowę obejmującą kemping słupka. | Karta to identyfikator posiadacza; posiadacz jest w relacji z jednym wydawcą; pytanie brzmi: czy ten wydawca ma dziś uprawnienie „akceptacja kart” wobec kempingu słupka. Wychodzi tak/nie oraz źródło: własna umowa z tym kempingiem albo umowa najbliższej organizacji nadrzędnej z nim, i do kiedy obowiązuje. Region w tej odpowiedzi nie występuje. <details><summary>szczegóły</summary>identyfikator KARTA (ważny) → `PartyId` posiadacza → OBSŁUGA KART WYDAWCY → Wydawca; uprawnienie AKCEPTACJA KART (Wydawca → Kemping) ścieżką: UMOWA AKCEPTACJI wprost | PODLEGŁOŚĆ WYDAWCY → UMOWA AKCEPTACJI najbliższej organizacji z własnymi umowami (zastępowanie, jak dziś); umowa „na region” to już wachlarz umów z kempingami, więc nie trzeba chodzić po przynależności; słupek → kemping to rzut spoza modelu; nieważny identyfikator = brak posiadacza; zajętość, budżet, QR — poza modelem.</details> |
| **ValidationService.validateFor (Q4):** Do gniazda przyłożono kartę: czy to aktywna karta personelu kempingu tego słupka albo organizacji nad nim? Jeśli tak, jest wpuszczana od razu, bez sprawdzania umowy wydawcy i ważności. | `ValidationService.java:261` — zaraz po sprawdzeniu słupka `houseCardService.getHouseCardForPedestal(kod, UID)`: pierwsza aktywna karta domowa o tym UID z grupy słupka i jej przodków; nie-null → `setHouseCard`, ACCEPTED, natychmiastowy powrót. Zwraca `HouseCard | null` — karta personelu o tym numerze, ważna na tym słupku. | Identyfikator karty wskazuje posiadacza; jeśli posiadacz (osoba z personelu, albo sam kemping/właściciel, gdy karta nie ma osoby) ma uprawnienie „ładowanie służbowe” wobec kempingu słupka — z relacji personelu z tym właśnie kempingiem — odpowiedź tak, ze źródłem i ważnością relacji. Karta „na region” ma taką relację z każdym kempingiem, który był w regionie, gdy ją wydano; „organizacja nad kempingiem” nie jest tu źródłem prawa. Omijanie dalszych sprawdzeń to decyzja procesu, nie modelu. <details><summary>szczegóły</summary>KARTA → posiadacz → uprawnienie ŁADOWANIE SŁUŻBOWE (posiadacz → Kemping) z PERSONEL (Kemping → Person) wprost; dla przygarniętej karty posiadacz = kemping/właściciel (PROWADZENIE KEMPINGU); „aktywna” = ważność identyfikatora i ważność relacji PERSONEL (sezon). Zakres węższy niż kemping — poza modelem (P4).</details> |
| **GroupService.findIssuerGroupIdsAcceptedAt (Q2):** Karty których wydawców przyjmuje kemping tego słupka? | Nie ma w bolączkach jako osobnego wołającego (występuje tylko w JAK wpisu LocalCardListService.buildLocalCardList, analiza/bolaczki-party.md:62). `campsite-core/src/main/java/io/pillopl/group/GroupService.java:309` — wymaga grupy SITE (inaczej `IllegalStateException`), rekurencyjny SQL `groupDao.findGroupIdsAcceptedAt(siteId, ISSUER, SITE)`: dla każdego wydawcy najbliższy w jego linii właściciel listy umów SITE; wydawca przyjęty, gdy umowa obejmuje grupę słupka albo jej przodka. Zwraca `List<Long>` — id wydawców, których karty działają na kempingu. | Odwrotne pytanie sieciowe: wszyscy wydawcy, którzy mają dziś uprawnienie „akceptacja kart” wobec tego kempingu — wprost albo po organizacji nadrzędnej. Wychodzi lista podmiotów-wydawców, każdy ze źródłem; przodkowie kempingu w strukturze sieci nie mają w tym udziału. <details><summary>szczegóły</summary>zbiór posiadaczy uprawnienia AKCEPTACJA KART wobec Kemping K; ścieżki jak w Q1; ta sama karta uprawnień, czytana od strony kempingu; wymaga, by K miał rolę KEMPING — inaczej pytanie bez sensu (dziś wyjątek).</details> |
| **LocalCardListService.buildLocalCardList (Q2):** Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: karty których wydawców przyjmuje kemping tego słupka? Karty wydawców spoza tej odpowiedzi nie trafiają na słupek. | `campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138` — gdy `lla.isIncludeCards()`: `groupService.findIssuerGroupIdsAcceptedAt(pedestal.getGroup())` → `campingCardDao.findValidCardsForLocalCardList(ids, dziś)`; grupa nie-SITE → `IllegalStateException`, brak grupy → NPE. Zwraca `List<Long>` — wydawcy, których umowa (własna albo przejęta) obejmuje kemping słupka. | Wydawcy z uprawnieniem „akceptacja kart” wobec kempingu, a potem ich posiadacze i identyfikatory kart ważne dziś. Model daje skład; które z nich zmieszczą się w pamięci słupka — nie odpowiada. <details><summary>szczegóły</summary>uprawnienie AKCEPTACJA KART wobec K → wydawcy → OBSŁUGA KART WYDAWCY → posiadacze → identyfikatory KARTA z ważnością obejmującą dziś; pojemność i ucinanie poza modelem (POJĘCIA NIEZMAPOWANE: lista lokalna).</details> |
| **LocalCardListService.buildLocalCardList (Q4):** Słupek dostaje listę kart, które ma wpuszczać sam: które karty personelu (kempingu i organizacji nad nim) mają na niej być? Mają pierwszeństwo przed kartami gości. | `LocalCardListService.java:138` — gdy `lla.isIncludeHouseCards()`: `houseCardService.getHouseCardsForPedestal(kod)` = aktywne karty grupy słupka i wszystkich przodków; karty personelu pierwsze; gdy same przekraczają pojemność, karty gości wypadają, personel sortowany po użyciu i ucinany. Zwraca `List<HouseCard>` — aktywne karty personelu kempingu słupka i organizacji nad nim. | Wszyscy posiadacze z uprawnieniem „ładowanie służbowe” wobec tego kempingu — z relacji personelu zawartych z nim (w tym tych, które powstały z karty „na region”) — i ich identyfikatory kart ważne dziś. Jednostki nad kempingiem nie dodają nikogo. Pierwszeństwo i pojemność — poza modelem. <details><summary>szczegóły</summary>posiadacze uprawnienia ŁADOWANIE SŁUŻBOWE wobec K: PERSONEL(K → Person) ∪ kemping/właściciel jako posiadacz kart przygarniętych; identyfikatory KARTA ważne dziś; relacje PERSONEL z ważnością (sezon) wypadają same po terminie.</details> |
| **LocalCardListService.buildLocalCardList (Q12, :124):** Osoba z obsługi technicznej podgląda, jakie karty ten słupek ma teraz na liście kart przyjmowanych bez pytania centrali (karty obsługi, karty gości) i ile się nie zmieściło. Jeśli w tej części sieci słupki nie trzymają listy, widzi pustą. | `LocalCardListService.java:124` — `getPedestal` (brak → `IllegalArgumentException`), `localListAttributeService.getLocalListAttribute(pedestal.getGroup())` (najbliższy atrybut w górę, tylko gdy `localListAllowed`); null → `LocalCardList.empty(code, capacity)`; inaczej prywatny `buildLocalCardList(pedestal, lla)`. Zwraca `LocalCardList` — karty obsługi, potem karty gości wydawców przyjmowanych, przycięte do pojemności. | Czy w tej części sieci słupki trzymają listę i jakie rodzaje kart — archetyp na to nie odpowiada: to ustawienie jednostki (kempingu/regionu), nie relacja ani rola (POJĘCIA NIEZMAPOWANE). Odpowiada tylko na skład: kto ma dziś uprawnienie ładowania służbowego i akceptacji kart wobec tego kempingu. <details><summary>szczegóły</summary>ustawienie listy lokalnej orbituje poza rdzeniem; jego dziedziczenie „najbliższe wyżej” może korzystać z PRZYNALEŻNOŚCI W SIECI jako ścieżki (to ustawienie, nie prawo ładowania); skład listy = wiersze Q2 + Q4 dla tego kempingu; pojemność z typu słupka poza modelem.</details> |
| **LocalCardListService.buildLocalCardList (Q12, :138):** Słupek dostaje od nowa całą listę kart przyjmowanych bez pytania centrali — na żądanie obsługi albo przy rozsyłce do wszystkich słupków: jakie karty mają na niej być i które wypadają, gdy brakuje miejsca? | `LocalCardListService.java:138` — wołane z `sendFull` (l. 81, po `getLocalListAttribute`, null → `false`) i z l. 124: `includeHouseCards` → `getHouseCardsForPedestal`; `includeCards` → `findValidCardsForLocalCardList(findIssuerGroupIdsAcceptedAt(grupa słupka), dziś)`; ponad pojemność: same karty domowe > pojemność → karty gości zerowane i `sortAndCutHouseCards`, inaczej `cutCards`. Zwraca `LocalCardList` — karty obsługi (ostatnio używane najpierw), potem karty gości (najnowsze najpierw). | Skład listy to suma dwu odpowiedzi modelu: posiadacze z „ładowaniem służbowym” wobec kempingu i posiadacze wydawców z „akceptacją kart” wobec kempingu, z identyfikatorami ważnymi dziś. Kolejność, ucinanie, pojemność i wysyłka — archetyp na to nie odpowiada. <details><summary>szczegóły</summary>jak wiersze Q2 i Q4; zmiana którejkolwiek relacji (UMOWA AKCEPTACJI, PERSONEL) albo identyfikatora KARTA emituje zdarzenie, na które rozsyłka może reagować; przeniesienie kempingu (PRZYNALEŻNOŚĆ) nie zmienia składu, więc nie wymaga rozsyłki; dziś zdarzenie istnieje tylko dla kart domowych.</details> |
| **WelcomeCardAdoptionService.checkPedestal (Q10):** Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli. | `campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:133` — tylko z `checkAndAdopt`: `adoptionIsAllowedFor(pedestal.getGroup())` (rekurencja po rodzicach, pierwszy atrybut `WelcomeCardAdoption`, brak → false) && `getAffiliateGroup` (pierwszy przodek z nazwą „AFFILIATE …”) != null && `affiliateGroupService.checkAccessMode(id) == HOUSE_CARDS_ONLY` (prefiks nazwy rodzica). Zwraca `boolean` — tak: kemping partnerski słupka zgadza się na przejmowanie i przyjmuje tylko własne karty. | Dwa z trzech warunków to pytania o podmioty: czy kemping ma właściciela-afilianta (relacja prowadzenia, właściciel inny niż sieć) i czy „przyjmuje tylko własne karty” — czyli czy sieć jako wydawca nie ma z nim umowy akceptacji. Trzeci, zgoda na przejmowanie — ustawienie jednostki, archetyp na to nie odpowiada. Samo przejęcie to przerejestrowanie identyfikatora karty z puli rynku na kemping/właściciela; karta nie jest kasowana, zmienia posiadacza. <details><summary>szczegóły</summary>PROWADZENIE KEMPINGU (Właściciel → K), właściciel ≠ sieć; brak UMOWY AKCEPTACJI (sieć-WYDAWCA → K) [wariant roboczy, zob. ZAŁOŻENIA]; „karta z puli kraju kempingu” (to `findWelcomeCard`, nie `checkPedestal`): posiadacz identyfikatora KARTA = PULA rynku R, gdzie R = rynek K odczytany po PRZYNALEŻNOŚCI W SIECI (etykieta, nie prawo); przejęcie: identyfikator KARTA z PULA → K (lub właściciel), przez politykę przydziału, ze zdarzeniem; przeniesienie K do innego regionu w tym samym rynku nie zmienia odpowiedzi; zgoda na przejmowanie poza modelem.</details> |
| **WelcomeCardAdoptionService.checkPedestal (Q11):** Gość przyłożył kartę powitalną do słupka i został odrzucony: czy ten niezależny kemping przyjmuje tylko własne karty obsługi i pozwala przygarniać karty powitalne? Jeśli tak, karta staje się kartą obsługi kempingu; jeśli kemping jest otwarty na karty sieci, karta zostaje odrzucona i nic się nie zmienia. | `WelcomeCardAdoptionService.java:133` — jak wyżej: łańcuch `Authorization.checkForCAMP84` (status `CARD_NOT_ACCEPTED_AT_SITE`) → zdarzenie → `checkAndAdopt` → `checkCard(findWelcomeCard) && checkPedestal`; tryb z prefiksu nazwy rodzica grupy afiliata („NETWORK ACCESS <MKT>” / „HOUSE CARDS ONLY <MKT>”). Zwraca `boolean` — tak: kemping pozwala przygarniać i przyjmuje tylko własne karty obsługi. | Tryb „otwarty na karty sieci / tylko własne karty” nie jest nazwą rodzica, lecz faktem o dwu podmiotach: czy sieć jako wydawca ma z tym kempingiem umowę akceptacji. Jest umowa — kemping otwarty, karta odrzucona i nic więcej; nie ma — kemping przyjmuje tylko własne, a dalej decyduje zgoda na przejmowanie (poza modelem) i to, czy posiadaczem karty jest pula tego rynku. <details><summary>szczegóły</summary>uprawnienie AKCEPTACJA KART (sieć-WYDAWCA → K) istnieje ⇔ NETWORK_ACCESS; brak ⇔ HOUSE_CARDS_ONLY (wariant roboczy; alternatywa: rola globalna kempingu); przełączenie trybu przez operatora = rozpoczęcie/zakończenie tej relacji (zdarzenie), bez przenoszenia w drzewie; zgoda na przejmowanie i flagi słupków poza modelem.</details> |
| **IssuerCardImportService.importCard (Q16):** Partner przysyła zmiany na liście swoich kart: czy ta karta to karta sieci albo karta innego wydawcy, której ten partner nie może przejąć ani zmienić? Jeśli tak, ta jedna zmiana jest odrzucona, pozostałe przechodzą. | Bez własnego wpisu w bolączkach (cel ścieżki IssuerExchangeService.importCards, analiza/bolaczki-party.md:209–212). `campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:72` — `findByCardRfid`; karta istnieje i `isNetworkCard` (grupa gościa w poddrzewie „NETWORK CARD”) → `IssuerExchangeException`; ADD/MODIFY/DELETE na karcie wydawcy (`isIssuerCard`) → `checkIssuer`: grupa gościa karty w `findManageable(grupa wydawcy partnera)`, inaczej „imported from another issuer already”; MODIFY/DELETE nieznanej → „card not found”; ADD nieznanej → nowa karta na gościu partnera (`getPartnerGuest`). Zwraca `CampingCard` (wołający ignoruje) — odrzucenie to wyjątek. | Identyfikator karty wskazuje posiadacza; posiadacz ma dokładnie jednego wydawcę. Partner ma uprawnienie „zmiana kart” tylko wobec posiadaczy, których wydawcą jest on sam albo wydawca pod nim; posiadaczy sieci nie zmienia nikt poza siecią. Nieznana karta przy dodaniu to nowy identyfikator na posiadaczu partnera. Przesunięcie czegokolwiek w drzewie tej odpowiedzi nie zmienia, bo relacja posiadacz–wydawca jest własnym obiektem, nie pozycją w drzewie. <details><summary>szczegóły</summary>KARTA → posiadacz → OBSŁUGA KART WYDAWCY → Wydawca W; uprawnienie ZMIANA KART (Partner → posiadacz) z OBSŁUGI KART WYDAWCY wprost albo PODLEGŁOŚĆ WYDAWCY (Partner → … → W); W = sieć (lub jednostka sieci) → brak uprawnienia zawsze; „karta sieci” = posiadacz w OBSŁUDZE KART z siecią-wydawcą lub jej jednostką, bez nazwy „NETWORK CARD”; nowy identyfikator → posiadacz zastępczy partnera (ZAŁOŻENIA); MODIFY/DELETE nieznanej — brak identyfikatora = brak posiadacza, odrzucenie poza modelem.</details> |
| **IssuerCardImportService.importCard (Q15):** Partner przysyła zmiany na liście swoich kart: czy od tego partnera-wydawcy w ogóle wolno przyjmować karty? Jeśli nie, żadna jego karta nie zostaje przyjęta. | Bez własnego wpisu (wołający wpisu IssuerCardImportService.checkWhitelistSynchronizable, analiza/bolaczki-party.md:197–200). `IssuerCardImportService.java:72–73` — pierwszy krok `checkWhitelistSynchronizable(partner)` → `WhitelistFilterService.isSynchronizableForImport(partner.getIssuerGroup(), partner)`: najbliższy filtr importowy w górę drzewa wydawców celujący w partnera albo we wszystkich; brak = tak; false → `IssuerExchangeException("…not synchronizable")` dla każdej linii osobno; partner bez grupy wydawcy — bez sprawdzenia. `void, throws IssuerExchangeException`. | Uprawnienie partnera „przyjmowanie kart” wobec sieci, wyprowadzone z relacji wymiany kart (kierunek: przyjmowanie) zawartej z partnerem albo z najbliższą organizacją nad nim w drzewie wydawców. Brak jakiejkolwiek relacji — rozstrzyga polityka domyślna (dziś: wolno). Wychodzi tak/nie ze źródłem, raz na partnera, nie na linię. <details><summary>szczegóły</summary>PRZYJMOWANIE KART (Partner → Sieć) z WYMIANA KART kierunek przyjmowanie, ścieżka wprost | PODLEGŁOŚĆ WYDAWCY → WYMIANA KART; „filtr celujący we wszystkich” = relacja z siecią na jednostce nadrzędnej obejmująca podległych; partner musi mieć rolę WYDAWCA (dziś partner bez grupy wydawcy omija sprawdzenie — w modelu brak roli = brak relacji = polityka domyślna, ale jawnie).</details> |

#### Trzy ficzery z archetypu

1. Odpowiedź na dzień wstecz: „czy karta wydawcy W miała prawo ładować na kempingu K w dniu D” — dla reklamacji z zeszłego sezonu bez przywracania zdezaktywowanego regionu i bez zgadywania, jak wtedy wyglądało drzewo.
   skąd: „Operator wycofuje stary region; z zeszłego sezonu wraca reklamacja gościa i nikt nie może otworzyć potrzebnych rekordów bez przywrócenia regionu” (analiza/archetypy.md:130)
   w archetypie: relacje (UMOWA AKCEPTACJI, PODLEGŁOŚĆ WYDAWCY, PERSONEL, PRZYNALEŻNOŚĆ W SIECI) i identyfikatory KARTA mają `Validity` i historię; karta uprawnień liczona na datę D zamiast „teraz” daje odpowiedź ze źródłem, a zakończona relacja nie znika, tylko ma datę końca. Ponieważ prawa ładowania nie zależą od przynależności, odpowiedź nie wymaga nawet odtwarzania regionu z tamtego dnia.
2. Personel jako osoby z terminem, nie karty na zawsze: pracownik sezonowy dostaje relację z kempingiem „od 1 czerwca do 30 września”; po terminie jego karta sama przestaje ładować, zgubiona karta jest blokowana, a nowa trafia na tę samą osobę bez zakładania nowej „karty obsługi”.
   skąd: „Dziś jedyna możliwość to karta obsługi na cały kemping, co jest «o wiele za dużo»” (analiza/archetypy.md:111); „karta kempingu dla obsługi” (analiza/archetypy.md:148)
   w archetypie: `Person` z relacją PERSONEL (Kemping → Person) o ważności od–do i rolą z katalogu; karta to identyfikator KARTA tej osoby z własną ważnością; uprawnienie ŁADOWANIE SŁUŻBOWE wygasa razem z relacją, bez ręcznego wyłączania karty, a lista „kto z obsługi ma dziś prawo ładować na K” to zwykłe pytanie po rolach. Ekipa remontowa z zestawem kempingów zmienianym co miesiąc to wiele relacji PERSONEL o ważności miesięcznej na jednej osobie.
3. Kontrola spójności sieci po reorganizacji: lista kempingów, na których nie działa żadna karta gościa (brak jakiegokolwiek wydawcy z akceptacją), wydawców, których karty nie działają nigdzie, posiadaczy bez wydawcy i kempingów bez przynależności do rynku — do przejrzenia przed sezonem i po każdym „podpisaniu kempingu”.
   skąd: „Gdy partner podpisuje nowy kemping… dziś trzeba «podłubać w kilku miejscach» (5–6 kroków), zawsze któryś się gubi i karty działają tylko w jedną stronę” (analiza/archetypy.md:110); „kilka regionów «wisiało bez rynku»” (analiza/archetypy.md:126)
   w archetypie: podmioty to wierzchołki, relacje to krawędzie — pytania o wierzchołki odcięte (kemping bez krawędzi UMOWA AKCEPTACJI ani PERSONEL, wydawca bez umowy wprost i po PODLEGŁOŚCI, posiadacz bez OBSŁUGI KART WYDAWCY, kemping bez PRZYNALEŻNOŚCI) to standardowe pytania sieciowe na tym samym grafie, który odpowiada na autoryzację.

### Pojęcia niezmapowane, zbiorczo

Jest jedno wystąpienie, więc żadne odrzucone pojęcie nie jest pokryte przez inne wystąpienie. Wszystkie poniżej to fragmenty biznesu poza archetypami.

| pojęcie | które wystąpienie je odrzuciło |
|---|---|
| słupek, gniazdo, kod słupka, sekcja i budżet mocy, zajętość gniazda — rzeczy i decyzje procesu autoryzacji; prawa węższe niż kemping (słupki 3–7) nie są modelowane | party (jedyne wystąpienie); poza archetypami |
| lista lokalna słupka: czy ją trzymać, co na niej, pojemność, kolejność ucinania, rozsyłka | party (jedyne wystąpienie); poza archetypami |
| zgoda na przejmowanie kart powitalnych (ustawienie dziedziczone) | party (jedyne wystąpienie); poza archetypami |
| aktywacja karty, numer karty, konto rozliczeniowe, koszt partnera | party (jedyne wystąpienie); poza archetypami |
| płatność QR, sesja dostawy, rekord zużycia, meldunek/wymeldowanie | party (jedyne wystąpienie); poza archetypami |
| flagi słupków przy trybie dostępu (`listedInApp`, `remoteControl`, `settledByCampsite`), taryfa domyślna regionu | party (jedyne wystąpienie); poza archetypami |
| filtry białej listy jako mechanizm (to, co z nich zostaje, to relacja WYMIANA KART) | party (jedyne wystąpienie); poza archetypami |
| widoczność rekordów w raportach i rozliczeniu, rozliczenie do kempingu macierzystego | party (jedyne wystąpienie); poza archetypami |
| drzewo konsoli jako widok (Q20) | party (jedyne wystąpienie); poza archetypami |
| wymiana plików z partnerem jako proces (cykl, statusy kierunków, zliczanie odrzuceń) | party (jedyne wystąpienie); poza archetypami |

### Sprzeciwy wobec werdyktu

Brak. Mapper potwierdził, że wystąpienie daje się zmapować.

### Otwarte

Pytania bez odpowiedzi: brak (mapper: „PYTANIA DO UŻYTKOWNIKA: brak”).

Założenia zmieniające zachowanie biznesowe, do potwierdzenia przy implementacji:
- **P1 przyjęte z zastrzeżeniem użytkownika „nie mam pewności”:** region i rynek to jednostki raportowe; z przynależności w sieci nie wynika żadne prawo ładowania. Umowa „na region” i karta personelu „na region” to wachlarz relacji z kempingami należącymi do regionu w chwili zawarcia lub wydania: kemping dopisany później ich nie dostaje, kemping przeniesiony ich nie traci. Jeśli biznes zdecyduje inaczej, zmieniają się tylko ścieżki uprawnień AKCEPTACJA KART i ŁADOWANIE SŁUŻBOWE oraz rola MIEJSCE AKCEPTACJI.
- Jedyne uprawnienie idące po przynależności w sieci to WGLĄD ról raportowych (back office, administrator rynku); przeniesienie kempingu zmienia tylko to, kto go widzi w konsoli.
- P2 „jak dziś”: umowa akceptacji zawierana z kempingiem; kto poza operatorem sieci może ją dodać lub wypowiedzieć, poza modelem do czasu decyzji.
- P3 „jak dziś”: polityka zastępowania umów po podległości wydawcy; zmiana na sumę to wymiana polityki, nie modelu.
- P4: prawa ładowania personelu zawsze na cały kemping; „słupki 3–7” nieobsługiwane, „zestaw kempingów zmieniany co miesiąc” to wiele relacji PERSONEL z ważnością miesięczną.
- Tryb „otwarty na karty sieci / tylko własne karty” kempingu niezależnego zamodelowany jako obecność albo brak umowy akceptacji sieci-wydawcy z tym kempingiem; alternatywa: rola globalna kempingu.
- Karta obsługi bez osoby to identyfikator zarejestrowany na kempingu lub właścicielu; karty importowane od partnera bez osoby wiszą na jednym posiadaczu zastępczym partnera.
- Umowy akceptacji dostają ważność od–do (dziś wszystkie otwarte); brak ustawienia wymiany z partnerem = wolno (polityka domyślna poza modelem).

### Bolączki z adresami w kodzie (wejście pogłębienia)

### BOLĄCZKI Z ADRESAMI W KODZIE

#### B1. Przeniesienie kempingu w drzewie
- co boli: Przeniesienie, scalenie albo zamknięcie kempingu czy regionu w konsoli (technicznie jedno pole rodzica) po cichu zmienia, czyje karty dostają prąd i kto widzi rekordy: gość dostaje odmowę bez powodu, przygarnięta karta przestaje działać, admin rynku traci połowę kempingów i stoi fakturowanie; poprawki są ręczne, bez wyjaśnienia przyczyny.
- kiedy: przeniesienie kempingu do innego regionu (przeciągnięcie w konsoli), także scalenie dwóch kempingów albo dezaktywacja starego regionu
- cel obserwowalny: kemping raportowany w regionie, w którym biznes chce go widzieć, bez zmiany tego, kto na nim ładuje i kto widzi jego rekordy
- zgłoszenia: OPS-202 (#347): po jesiennej reorganizacji admin.it nie widzi przeniesionych kempingów, fakturowanie stoi (otwarte, pilne); CAMP-203 (#3): po przeniesieniu Zadaru pod Dalmację back office nie widzi jego rekordów; „będziemy dalej reorganizować”; CAMP-207 (#76): dezaktywacja starego regionu ukryła rekordy potrzebne do otwartej reklamacji; RPT-212 (#318): dwie osoby z tą samą rolą rynku widzą inne sumy, zależnie od miejsca w drzewie (otwarte); CAMP-212 (#334): karta gościa odrzucana bez powodu kilka razy w tygodniu po przesunięciach; wsparcie nie umie wyjaśnić (otwarte); CAMP-233 (#193): przygarnięta karta powitalna przestaje działać po przeniesieniu kempingu; pomaga ponowne przygarnięcie; OPS-205 (#336): po przeniesieniu raz tak, raz nie wiadomo, kto z obsługi może ładować; operacje chcą reguły albo ostrzeżenia; CAMP-211 (#337): dozorca z uprawnieniem na regionie ładuje też u sąsiada z tego samego regionu; CAMP-219 (#206): po scaleniu dwóch kempingów goście starego B dalej mają odmowę na słupkach A (otwarte); CRD-202 (#198): filtr kart znika po zapisie grupy, częściej na niedawno przeniesionych (wontfix); CAMP-232 (#231): domyślna taryfa regionu raz trafia do nowego kempingu, raz nie, zależnie od głębokości; wiki Konsultacje-2026-03-10 ¶3–4: Zadar: „technicznie to jest jedno pole w bazie […] a potem dzwonią”; widok przywrócony ręcznie; wiki Konsultacje-2026-03-10 ¶13, ¶14, ¶18, ¶22–24: dozorca u sąsiada, przygarnięta karta, scalenie, odmowy „częściej w tygodniach, jak biznes dużo przesuwa”, admin.it; wiki Konsultacje-2026-03-10 ¶26: PO: „żebyśmy mogli poprzesuwać kempingi […] i żeby nic się przy tym nie sypało”

#### B3. Zasięg kart w kilku miejscach
- co boli: Ustalenie, gdzie działa karta, wymaga dotknięcia kilku miejsc: umowa z wydawcą to listy węzłów po obu stronach i filtry na kilku poziomach, karta obsługi działa na całe poddrzewo. Przy nowym kempingu wydawcy zawsze któryś krok się gubi i karty działają w jedną stronę; nie da się dać karty na pięć słupków ani na zmieniający się co miesiąc zestaw kempingów; odmowa nie mówi, który filtr zadziałał.
- kiedy: wydawca podpisuje nowy kemping, dwa kempingi chcą wspólnie ładować, obsłudze trzeba dać kartę na część kempingu albo na kilka kempingów
- zgłoszenia: ISS-205 (#367): dodanie kempingu wydawcy to kilka ręcznych kroków, jeden się gubi i karty działają w jedną stronę (w toku); ISS-203 (#79): karty partnera mają działać od razu na kempingach z umową (wontfix); CAMP-215 (#399): Zadar i sąsiad: goście mają ładować u siebie nawzajem; nikt nie wie, jak to połączyć; CAMP-218 (#72): trzy kempingi Garda: ładowanie wspólne, rozliczenia i raporty osobno; CAMP-210 (#110): pracownik sezonowy tylko na słupkach 3–7, a da się tylko cały kemping; CRD-201 (#345): karta dla ekipy na zestaw kempingów zmieniany co miesiąc (otwarte); ISS-207 (#364): odmowa przez filtr nie mówi, którego poziomu to filtr (otwarte); wiki Konsultacje-2026-03-10 ¶13, ¶15, ¶17–19: sezonowi, ekipy remontowe, filtry, sąsiad na festiwal, Garda, „rytuał na pięć kroków i zawsze któryś się zgubi”; Glossary (acceptance agreement): umowa: karty wydawcy działają na danych kempingach

Bolączki B1 i B3 dotykają tej samej paczki pytań. Pod nimi pytania, dla których zrobiliśmy skrzyżowanie; przy każdym wołającym: miejsce w kodzie (`plik:linia`), CO (co jest robione, w języku problemu), JAK (jak kod robi to dziś) i odpowiedź jednym zdaniem (typ i co znaczy).

##### Paczka pytań: Czy ta karta dostanie prąd na tym kempingu

- zgłoszeń: 6 · dlaczego boli: gość i obsługa dostają odmowę (albo prąd u sąsiada) po zmianie, która miała dotyczyć tylko raportowania; wsparcie nie umie powiedzieć czemu
- co się zmienia: w gnieździe płynie prąd albo nie · kto zauważa: gość przy słupku, personel po odmowie

###### Q1: Czy wydawca karty gościa ma z siecią umowę, że jego karty działają na kempingu, na którym stoi ten słupek?

- **GroupApiService.toAgreementDTO** (`campsite-core/src/main/java/io/pillopl/group/GroupApiService.java:277`) · top-in-island · ścieżka: GroupApiService.toAgreementDTO → GroupServiceHelper.getAcceptanceAgreements · pyta: partly
  - CO: Operator sieci w konsoli ogląda albo zmienia umowy wydawcy kart: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej?
  - JAK: `toAgreementDTO(issuer)` liczy cały zasięg wydawcy, a nie odpowiedź dla jednego słupka: `GroupServiceHelper.getAcceptanceAgreements(issuer, SITE)` (najbliższy na linii rodziców wydawca z niepustą listą umów SITE; listy wyżej są zastępowane, nie łączone), flaga `inherited = !own && !agreements.isEmpty()` (own = wydawca sam ma choć jedną grupę SITE w `getGroups()`), oraz `extractAllAcceptingSiteGroupIds(issuer)` = id całych poddrzew kempingów z umów. Wołane po `findAcceptanceAgreements` (podgląd), `addAcceptanceAgreement` i `removeAcceptanceAgreement` (wynik po zmianie). Pętla po `issuer.getGroups()` nie sprawdza null (helper sprawdza).
  - odpowiedź: `AcceptanceAgreementDTO` — zasięg kart jednego wydawcy: umowy obowiązujące wydawcę (kempingi/regiony wpisane w umowę) i wszystkie kempingi pod nimi

- **GroupService.extractAllAcceptingSiteGroupIds** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:291`) · top-in-island · ścieżka: GroupService.extractAllAcceptingSiteGroupIds → GroupServiceHelper.extractAllAcceptingSiteGroupIds · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: na których kempingach działają karty tego wydawcy?
  - JAK: `GroupService.extractAllAcceptingSiteGroupIds(issuerGroup)` to transakcyjna obwoluta `GroupServiceHelper.extractAllAcceptingSiteGroupIds`; grep po kodzie produkcyjnym nie znajduje wołających (jest bliźniacza `findAcceptingSiteGroupIds` liczona rekurencyjnym SQL). Produkcja woła helper statycznie, z pominięciem tej metody.
  - odpowiedź: `List<Long>` — lista: kempingi (i regiony) pod umowami obowiązującymi wydawcę

- **GroupService.isAcceptedAt** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:320`) · top-in-island · ścieżka: GroupService.isAcceptedAt → GroupServiceHelper.extractAllAcceptingSiteGroupIds · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: czy karty tego wydawcy działają na tym kempingu?
  - JAK: `isAcceptedAt(issuerGroup, siteGroupId)`: null w którymkolwiek argumencie → false; inaczej `GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup).contains(siteGroupId)`. Brak wołających w kodzie produkcyjnym.
  - odpowiedź: `boolean` — tak: kemping leży pod jedną z umów obowiązujących wydawcę

- **GroupService.isCardAcceptedAt** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:348`) · top-in-island · ścieżka: GroupService.isCardAcceptedAt · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na to samo pytanie co przy przyłożeniu karty: czy wydawca karty gościa ma umowę obejmującą kemping tego słupka?
  - JAK: Wariant po kodzie słupka: karta bez gościa → false; inaczej `extractAllAcceptingSiteGroupIds(guest.group)` i `pedestalDao.findByCodeAndGroupIds(kod, ids)` (słupek nieskasowany w jednej z grup). Produkcja (ValidationService) woła wariant po id słupka `isCardAcceptedAt(Long, CampingCard)`, ten — nikt.
  - odpowiedź: `boolean` — tak: słupek stoi na kempingu objętym umową wydawcy karty

- **GroupServiceHelper.extractAllAcceptingGroupIds** (`campsite-core/src/main/java/io/pillopl/group/GroupServiceHelper.java:52`) · top-in-island · ścieżka: GroupServiceHelper.extractAllAcceptingGroupIds · pyta: partly
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Ogólny krok „umowy → poddrzewa → id” dla dowolnego typu celu; w produkcji wołany tylko przez `extractAllAcceptingSiteGroupIds` (typ SITE), a ten przez `GroupService.isCardAcceptedAt(Long, CampingCard)` (przyłożenie karty) i `GroupApiService.toAgreementDTO` (ekran umów). Bierze `getAcceptanceAgreements` (najbliższa w linii rodziców niepusta lista umów danego typu), dla każdej umowy `collectSubtreeIds` i składa listę bez powtórzeń; brak umów → pusta lista. Sam nie zna słupka — odpowiada na „gdzie wolno”, nie „czy tu”.
  - odpowiedź: `List<Long>` — lista: kempingi i regiony (z całym ich poddrzewem), na których działają karty wydawcy

- **ValidationService.validateFor** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:261`) · top-in-island · ścieżka: ValidationService.validateFor · pyta: yes
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Łańcuch pierwszy-błąd w prywatnym `validateFor`: słupek istnieje → karta personelu (Q4, wczesne ACCEPTED) → znacznik płatności QR → karta znana sieci → jeśli `withoutCardValidation` (dostawa już trwa) tylko czy jest gość, bez pytania o umowę → `isCardAcceptedAt(card, pedestal)`; false → status `CARD_NOT_ACCEPTED_AT_SITE` i powrót; true → ważność karty na chwilę zgłoszenia (`card.validate(remoteDate)` → `translate`). Pytanie pada przy autoryzacji ze słupka (`Authorization.authenticate` przez `validateForNow`), przy starcie dostawy (`SupplyControlUnitImpl`, `validateFor(..., false)`), przy zatrzymaniu inną kartą i przy sprawdzeniu karty w konsoli (`CampingCardService.checkCardAtSocket`).
  - odpowiedź: `boolean` — tak: wydawca karty ma umowę obejmującą kemping słupka

- **ValidationService.isCardAcceptedAt** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:381`) · top-in-island · ścieżka: ValidationService.isCardAcceptedAt · pyta: yes
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Przelotka: `groupService.isCardAcceptedAt(pedestal.getId(), card)`. Tam: karta bez gościa → false; `extractAllAcceptingSiteGroupIds(guest.group)`; pusta → false; inaczej `pedestalDao.isInGroupList(pedestalId, ids)` (słupek nieskasowany, którego grupa jest na liście) != null. Wynik wraca do `validateFor`.
  - odpowiedź: `boolean` — tak: słupek stoi na kempingu objętym umową wydawcy karty

###### Q2: Karty których wydawców przyjmuje kemping tego słupka?

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: karty których wydawców przyjmuje kemping tego słupka? Karty wydawców spoza tej odpowiedzi nie trafiają na słupek.
  - JAK: Gdy atrybut listy lokalnej przyjmuje karty gości (`lla.isIncludeCards()`), `groupService.findIssuerGroupIdsAcceptedAt(pedestal.getGroup())` (rekurencyjny SQL `findGroupIdsAcceptedAt`: dla każdego wydawcy najbliższy w jego linii właściciel listy umów SITE; wydawca przyjęty, gdy umowa obejmuje grupę słupka albo jej przodka) → `campingCardDao.findValidCardsForLocalCardList(ids, dziś)`. Karty personelu idą pierwsze; przy przekroczeniu pojemności karty gości są ucinane. Grupa słupka nie-SITE → `IllegalStateException`, brak grupy → NPE z `requireNonNull` (nie łapane tutaj). Lista idzie na słupek (`sendFull`: rozsyłka automatyczna i ręczna z konsoli) albo do podglądu w konsoli (`getLocalCardList`).
  - odpowiedź: `List<Long>` — lista: wydawcy kart, których umowa (własna albo przejęta od organizacji nadrzędnej) obejmuje kemping słupka

###### Q3: Na których kempingach mogą ładować karty tego wydawcy (tego gościa) i czy to umowa wydawcy, czy odziedziczona po organizacji nadrzędnej?

- **GroupWebService.findAcceptanceAgreements** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:45`) · caller · wejście HTTP · ścieżka: GroupWebService.findAcceptanceAgreements → GroupApiService.findAcceptanceAgreements · pyta: yes
  - CO: Operator sieci w konsoli ogląda mapę organizacji: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej? Wydawcy spoza swojego zakresu operator nie zobaczy.
  - JAK: POST `/findAcceptanceAgreements` → `GroupApi.findAcceptanceAgreements(caller, issuerGroupId)` → `GroupApiService`: `principalResolver.resolve`, `fetchManageable` (grupa musi istnieć i być w zakresie użytkownika w drzewie tej grupy: `isManageable`), `checkType(ISSUER)`, `toAgreementDTO`. Wyjątki tłumaczy `ApiExceptionTranslator`, endpoint opakowuje w `GroupWebServiceFault`. Konsola (`OrganisationPage`) rysuje umowy, przerywaną linią odziedziczone, i podświetla `acceptingSiteGroupIds`.
  - odpowiedź: `FindAcceptanceAgreementsResponse(AcceptanceAgreementDTO)` — zasięg kart wydawcy: obowiązujące umowy i wszystkie kempingi pod nimi

- **PedestalService.findPedestalsForGuest** (`campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:181`) · cited · ścieżka: PedestalService.findPedestalsForGuest · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: na których słupkach w moim zakresie może ładować ten gość?
  - JAK: `findPedestalsForGuest(userinfo, guest)`: użytkownik bez grupy SITE albo gość bez wydawcy → pusta lista; inaczej SQL `findPedestalIdsForGuestRestrictedToUserSiteGroup` (poddrzewo SITE użytkownika ∩ słupki pod umowami najbliższego w linii wydawcy właściciela umów). Grep po kodzie produkcyjnym nie znajduje wołających.
  - odpowiedź: `List<Pedestal>` — lista: słupki w zakresie użytkownika, na których działa karta gościa

- **GroupApiService.toAgreementDTO** (`campsite-core/src/main/java/io/pillopl/group/GroupApiService.java:277`) · top-in-island · ścieżka: GroupApiService.toAgreementDTO · pyta: yes
  - CO: Operator sieci w konsoli ogląda albo zmienia umowy wydawcy kart: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej?
  - JAK: `toAgreementDTO(issuer)`: `getAcceptanceAgreements(issuer, SITE)` (najbliższa w linii niepusta lista umów, wyższe zastępowane), `inherited = !own && !agreements.isEmpty()` (own = wydawca ma własną grupę SITE w `getGroups()`, bez sprawdzenia null), `acceptingSiteGroupIds = extractAllAcceptingSiteGroupIds(issuer)` (całe poddrzewa). Wołane po podglądzie oraz po dodaniu i usunięciu umowy.
  - odpowiedź: `AcceptanceAgreementDTO` — zasięg kart jednego wydawcy: obowiązujące umowy i wszystkie kempingi pod nimi

###### Q4: Czy to karta personelu, której wolno ładować na tym kempingu bez ograniczeń (i dla jakiego obszaru ją wydano)?

- **Authorization.authenticate** (`campsite-core/src/main/java/io/pillopl/card/Authorization.java:69`) · cited · ścieżka: Authorization.authenticate · pyta: yes
  - CO: Do gniazda przyłożono kartę personelu kempingu (albo organizacji nad nim): czy włączyć prąd od razu, bez sprawdzania, czy gniazdo jest zajęte i czy sekcja ma zapas mocy?
  - JAK: `authenticate(cardRfid, socketLinkId)` bierze `validationService.validateForNow` (tam `houseCardService.getHouseCardForPedestal` daje wczesne ACCEPTED z `houseCard`). Status != ACCEPTED → odmowa; `validationResult.isHouseCardAccepted()` (houseCard != null) → `houseCardService.recordUse` i ACCEPTED z pominięciem `supplySessionRegistry.hasConcurrentSession` i `sectionPowerBudget.isWithinSectionBudget`, bez tworzenia sesji; inaczej oba sprawdzenia i `createSession`. Wariant po kodzie słupka (101-110) tylko zapisuje użycie.
  - odpowiedź: `ValidationResult` — wynik sprawdzenia karty przy gnieździe; ważne, czy wskazuje kartę personelu

- **HouseCardApiService.saveHouseCard** (`campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:50`) · caller · ścieżka: HouseCardApiService.saveHouseCard → HouseCardService.saveHouseCard · pyta: partly
  - CO: Obsługa w konsoli wydaje albo zmienia kartę personelu kempingu: dla jakiego obszaru ta karta ma działać? Zawsze dla kempingu właściciela, w którego imieniu jest wydawana.
  - JAK: Nie pyta, czy karta jest kartą personelu tego słupka — ustala, gdzie później będzie. `HouseCardApiService.saveHouseCard`: uprawnienie `CARD_EDIT`, właściciel, typ, istniejąca karta (inaczej `ObjectNotFoundException`) → `HouseCardService.saveHouseCard`: `checkUserPrivileges`, brak typu/UID → `IllegalArgumentException`, zmieniana karta spoza poddrzewa kempingu właściciela → `AccessDeniedException`, `setGroup(getOwnerSiteGroup(owner))` (właściciel bez kempingu → `IllegalStateException`), zapis i zdarzenie `HOUSE_CARD_CHANGED` (odświeżenie list kart słupków). Wynik mapowany na `HouseCardDTO`.
  - odpowiedź: `HouseCard` — zapisana karta personelu z kempingiem, dla którego działa

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: które karty personelu (kempingu i organizacji nad nim) mają na niej być? Mają pierwszeństwo przed kartami gości.
  - JAK: Gdy atrybut listy lokalnej przyjmuje karty personelu (`lla.isIncludeHouseCards()`), `houseCardService.getHouseCardsForPedestal(kod)`: nieznany słupek albo bez grupy → pusta lista; inaczej `fetchParentLineIds(grupa słupka, true)` i `houseCardDao.findActiveByGroupIds` (aktywne karty grupy słupka i wszystkich przodków). Karty personelu idą pierwsze; gdy same przekraczają pojemność, karty gości wypadają w całości, a karty personelu są sortowane (ostatnio używane pierwsze) i ucinane.
  - odpowiedź: `List<HouseCard>` — lista: aktywne karty personelu kempingu słupka i organizacji nad nim

- **WelcomeCardAdoptionListener.handleEvent** (`campsite-core/src/main/java/io/pillopl/event/WelcomeCardAdoptionListener.java:37`) · caller · ścieżka: WelcomeCardAdoptionListener.handleEvent → WelcomeCardAdoptionService.checkAndAdopt → HouseCardService.saveHouseCard · pyta: partly
  - CO: Karta odrzucona na kempingu, bo jej wydawca nie ma tam umowy, okazuje się kartą powitalną z puli rynku: czy kemping partnerski przejmuje ją jako swoją kartę personelu? Jeśli tak, od następnego przyłożenia ładuje tam bez ograniczeń.
  - JAK: Zdarzenie `CHECK_WELCOME_CARD_ADOPTION` (wysłane przez `Authorization.checkForCAMP84` przy CARD_NOT_ACCEPTED_AT_SITE) → `welcomeCardAdoptionService.checkAndAdopt(kod słupka, UID)`: słupek nieznany/bez grupy → false; `findWelcomeCard` (karta gościa z grupy-puli kart powitalnych rynku) i `checkPedestal` (adopcja dozwolona atrybutem, jest kemping partnerski po prefiksie nazwy, tryb dostępu HOUSE_CARDS_ONLY) → `houseCardService.saveHouseCard(owner, owner, null, STAFF, …, true)` (karta dla kempingu właściciela partnera) i skasowanie karty powitalnej. Listener ignoruje zwrócony boolean, wyjątki tylko loguje.
  - odpowiedź: `boolean` — tak: karta powitalna przejęta jako karta personelu kempingu partnerskiego, karta gościa skasowana

- **ValidationService.validateFor** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:261`) · top-in-island · ścieżka: ValidationService.validateFor · pyta: yes
  - CO: Do gniazda przyłożono kartę: czy to aktywna karta personelu kempingu tego słupka albo organizacji nad nim? Jeśli tak, jest wpuszczana od razu, bez sprawdzania umowy wydawcy i ważności.
  - JAK: W prywatnym `validateFor`, zaraz po sprawdzeniu, że słupek istnieje: `houseCardService.getHouseCardForPedestal(kod, UID)` = pierwsza karta z `getHouseCardsForPedestal` (aktywne karty grupy słupka i przodków) o tym UID, inaczej null. Nie-null → `setHouseCard`, status ACCEPTED, natychmiastowy powrót (przed płatnością QR, kartą gościa, umową i ważnością); null → dalsze sprawdzenia.
  - odpowiedź: `HouseCard albo null` — karta personelu o tym numerze, ważna na tym słupku

###### Q10: Czy kemping chce przejąć kartę powitalną, którą gość zostawił, jako własną kartę obsługi — i czy to w ogóle karta powitalna z puli tego kraju?

- **Authorization.checkForCAMP84** (`campsite-core/src/main/java/io/pillopl/card/Authorization.java:58`) · cited · ścieżka: Authorization.checkForCAMP84 · pyta: partly
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `Authorization.authenticate` (oba warianty: gniazdo i sam słupek; wołane z protokołów słupka v1–v3 i płatności QR) zaraz po `validateForNow` woła `checkForCAMP84`. Ten pyta tylko o wstępny warunek: status `CARD_NOT_ACCEPTED_AT_SITE` → wysyła `CheckWelcomeCardAdoptionEvent(pedestalCode, cardRfid)`; każdy inny status → nic. Błąd wysłania jest logowany. Właściwe pytanie (pula, zgoda kempingu) zadaje później słuchacz, poza odpowiedzią dla słupka – słupek i tak dostaje odmowę.
  - odpowiedź: `void` — nic nie zwraca

- **CampingCardService.welcomeCardPools** (`campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:235`) · cited · ścieżka: CampingCardService.welcomeCardPools · pyta: partly
  - CO: Recepcja wydaje gościowi wolną kartę powitalną: czy ta karta naprawdę leży w puli kart powitalnych któregoś kraju i czy gość sam nie jest „gościem puli”? Jeśli nie, wydanie karty jest odrzucone.
  - JAK: Prywatne `CampingCardService.welcomeCardPools()` zbiera dla każdego `Market.values()` grupę puli `getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup()` (szukaną po nazwie w poddrzewie wydawców), pomija null. `isWelcomePool(group)` sprawdza, czy grupa jest jedną z nich; `isWelcomePoolCard(card)` – czy grupa gościa karty jest pulą. Klient: `assignWelcomeCard` (konsola, `CardApiService.assignWelcomeCard`): karta nie z puli albo nieaktywowana → `IllegalStateException`; gość należący do puli → `IllegalStateException`. Pyta o pulę dowolnego kraju, nie kraju kempingu – inaczej niż przy przejęciu karty na słupku.
  - odpowiedź: `List<Group>` — lista: pule kart powitalnych wszystkich krajów, które mają taką pulę

- **GroupWebService.getGroupAttributes** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:87`) · caller · wejście HTTP · ścieżka: GroupWebService.getGroupAttributes → GroupApiService.getGroupAttributes · pyta: partly
  - CO: Operator ogląda w konsoli ustawienia kempingu lub regionu: czy to miejsce przejmuje karty powitalne jako własne karty obsługi (własne ustawienie albo odziedziczone z góry)?
  - JAK: `/getGroupAttributes` → `GroupApiService.getGroupAttributes` (sprawdzenie zasięgu `fetchManageable`) → `welcomeCardAdoptionService.adoptionIsAllowedFor(group)`: rekurencyjnie w górę po rodzicach, pierwszy atrybut `WelcomeCardAdoption` decyduje (`isAllowed()`), brak do korzenia → false. Wynik w polu `welcomeCardAdoption`, `OrganisationPage.tsx` pokazuje yes/no. Pokazuje tylko zgodę kempingu – bez warunku „tylko własne karty” i bez puli kraju.
  - odpowiedź: `boolean` — tak: najbliższe ustawienie nad miejscem zgadza się na przejmowanie kart powitalnych

- **WelcomeCardAdoptionListener.handleEvent** (`campsite-core/src/main/java/io/pillopl/event/WelcomeCardAdoptionListener.java:37`) · cited · ścieżka: WelcomeCardAdoptionListener.handleEvent · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: Słuchacz `CHECK_WELCOME_CARD_ADOPTION` w osobnym zadaniu woła `welcomeCardAdoptionService.checkAndAdopt(pedestalCode, rfid)`: słupek nieznany/bez grupy → false; rynek z grupy słupka (`determineMarketByGroupId`); `findWelcomeCard` (karta gościa z puli tego rynku); `checkPedestal` (zgoda najbliższego przodka + kemping partnerski znaleziony po prefiksie nazwy + tryb `HOUSE_CARDS_ONLY`). Oba tak → `HouseCardService.saveHouseCard(owner, owner, null, STAFF, rfid, numer, true)` dla właściciela z nazwy kempingu i karta powitalna oznaczona jako usunięta. Wynik boolean jest ignorowany, wyjątki logowane.
  - odpowiedź: `boolean` — tak: karta była powitalna z puli kraju, kemping przejmuje takie karty i przyjmuje tylko własne – karta przejęta

- **WelcomeCardAdoptionService.findWelcomeCard** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:114`) · top-in-island · ścieżka: WelcomeCardAdoptionService.findWelcomeCard · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `findWelcomeCard(cardRfid, market)` (pakietowo-prywatne, woła je tylko `checkAndAdopt`): brak rynku lub UID → null; pula = `getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup()` (grupa wydawcy szukana po nazwie), brak → null; karta po UID, zwracana tylko gdy jej gość należy dokładnie do grupy puli. Rynek pochodzi z grupy słupka, więc rynek nieustalony (np. zła nazwa grupy) daje null i karta nie zostanie przejęta. `checkAndAdopt` traktuje null jako „nie karta z puli”.
  - odpowiedź: `CampingCard | null` — karta powitalna z puli kraju kempingu o tym UID

- **WelcomeCardAdoptionService.checkPedestal** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:133`) · top-in-island · ścieżka: WelcomeCardAdoptionService.checkPedestal · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `checkPedestal(pedestal)` (tylko z `checkAndAdopt`): `adoptionIsAllowedFor(pedestal.getGroup())` – rekurencja po rodzicach, pierwszy atrybut `WelcomeCardAdoption` decyduje, brak → false; `getAffiliateGroup` – pierwszy przodek z nazwą zaczynającą się od prefiksu kempingu partnerskiego; `affiliateGroupService.checkAccessMode(id) == HOUSE_CARDS_ONLY` (tryb wynika z nazwy rodzica, null → false). Wszystkie trzy muszą być prawdą.
  - odpowiedź: `boolean` — tak: kemping partnerski słupka zgadza się na przejmowanie kart powitalnych i przyjmuje tylko własne karty

- **WelcomeCardAdoptionService.getAffiliateGroup** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:149`) · top-in-island · ścieżka: WelcomeCardAdoptionService.getAffiliateGroup · pyta: partly
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: Statyczne `getAffiliateGroup(group)`: idzie w górę po rodzicach do pierwszej grupy, której nazwa zaczyna się od `PREFIX_AFFILIATE + " "`; brak → null. Używane w `checkPedestal` (czy słupek w ogóle stoi na kempingu partnerskim, potem tryb dostępu) i w `getAffiliateOwner` (właściciel = reszta nazwy po prefiksie; to on dostaje kartę obsługi w `checkAndAdopt`). Odpowiada na pod-pytanie „który kemping i czyj” – z nazwy grupy.
  - odpowiedź: `Group | null` — kemping partnerski, na którym stoi słupek

###### Q11: Czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi?

- **AffiliatePedestalService.switchAccessMode** (`campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:358`) · cited · ścieżka: AffiliatePedestalService.switchAccessMode · pyta: partly
  - CO: Instalator uruchamia słupki u właściciela niezależnego kempingu i ustala, czy kemping ma być otwarty na karty sieci (i widoczny w aplikacji), czy przyjmować tylko własne karty obsługi. Kemping trafia do odpowiedniej części sieci, a jego słupki dostają ustawienia zgodne z wyborem. Uruchomienie nie sprawdza, jaki tryb faktycznie wyszedł.
  - JAK: Prywatny `AffiliatePedestalService.switchAccessMode` woła `AffiliateGroupService.switchAccessMode(grupa, commissioning.getAccessMode(), commissioning.isListedInApp())` na końcu obu wariantów uruchomienia: `commission(List<Pedestal>, installer, token)` (l. 202) i `commission(pedestalCode, …, accessMode)` (l. 247). `AffiliateGroupService.switchAccessMode` przepina rodzica grupy na „NETWORK ACCESS <MKT>” albo „HOUSE CARDS ONLY <MKT>” i ustawia flagi słupków (NETWORK_ACCESS: `listedInApp`=`remoteControl`=wybór; inaczej: `listedInApp`, `remoteControl`, `settledByCampsite` = false), wysyła `AccessModeSwitchedEvent`, po czym odczytuje tryb ponownie przez `checkAccessMode(affiliateGroup)` (prefiks nazwy rodzica). Wynik jest ignorowany, metoda jest `void`; zaraz potem `commissioningService.finish(commissioning)`. Domyślny tryb w `Commissioning` to HOUSE_CARDS_ONLY.
  - odpowiedź: `void` — nic nie zwraca

- **GroupWebService.switchAccessMode** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:69`) · caller · wejście HTTP · ścieżka: GroupWebService.switchAccessMode → GroupApiService.switchAccessMode → AffiliateGroupService.switchAccessMode · pyta: partly
  - CO: Operator w konsoli przełącza niezależny kemping między „otwarty na karty sieci” a „tylko własne karty obsługi” i chce zobaczyć, jaki tryb kemping ma po zmianie. Jeśli kemping nie jest niezależny, leży poza jego zakresem albo tryb nie dał się zmienić – dostaje odmowę.
  - JAK: `GroupWebService.switchAccessMode` (POST /switchAccessMode) → `GroupApiService.switchAccessMode`: `principalResolver.resolve(…, CONFIG_EDIT)`, `fetchManageable` (grupa w zakresie operatora), `ApiValues.parseEnum(AccessMode, …)`, `AffiliateGroupService.isAffiliate(group)` (false → `ConflictException`), potem `AffiliateGroupService.switchAccessMode(group.getId(), targetMode, listedInApp)` – przepięcie rodzica + flagi słupków + `AccessModeSwitchedEvent`, odczyt trybu po zmianie z prefiksu nazwy rodzica. Wynik null → `ConflictException`; inaczej `result.name()` wraca w `SwitchAccessModeResponse`. Wyjątki tłumaczy `ApiExceptionTranslator`, endpoint opakowuje w `GroupWebServiceFault`.
  - odpowiedź: `SwitchAccessModeResponse, throws GroupWebServiceFault` — tryb przyjmowania kart, w jakim kemping jest po zmianie

- **GroupWebService.getGroupAttributes** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:87`) · caller · wejście HTTP · ścieżka: GroupWebService.getGroupAttributes → GroupApiService.getGroupAttributes · pyta: yes
  - CO: Operator ogląda ustawienia kempingu w konsoli: czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi? Przy kempingu, który nie jest niezależny (albo wisi w części sieci bez trybu), trybu nie widać.
  - JAK: `GroupWebService.getGroupAttributes` (POST /getGroupAttributes) → `GroupApiService.getGroupAttributes`: `fetchManageable`, potem `AffiliateGroupService.isAffiliate(group) ? AffiliateGroupService.checkAccessMode(group) : null` (statyczna wersja, bez logu: patrzy tylko na prefiks nazwy rodzica). Wynik `accessMode.name()` albo null idzie do pola `accessMode` w `GroupAttributesDTO` razem z pozostałymi atrybutami.
  - odpowiedź: `GetGroupAttributesResponse(GroupAttributesDTO), throws GroupWebServiceFault` — tryb przyjmowania kart tego kempingu, jeden z wielu atrybutów na ekranie

- **WelcomeCardAdoptionService.checkPedestal** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:133`) · top-in-island · ścieżka: WelcomeCardAdoptionService.checkPedestal · pyta: yes
  - CO: Gość przyłożył kartę powitalną do słupka i został odrzucony, bo kemping nie przyjmuje tej karty: czy ten niezależny kemping przyjmuje tylko własne karty obsługi i pozwala przygarniać karty powitalne? Jeśli tak, karta staje się kartą obsługi kempingu; jeśli nie, zostaje odrzucona i nic się nie zmienia.
  - JAK: Łańcuch: `Authorization.checkForCAMP84` przy statusie `CARD_NOT_ACCEPTED_AT_SITE` wysyła `CheckWelcomeCardAdoptionEvent` → `WelcomeCardAdoptionListener.handleEvent` → `WelcomeCardAdoptionService.checkAndAdopt(pedestalCode, rfid)`. Tam `checkCard(findWelcomeCard(…)) && checkPedestal(pedestal)`. `checkPedestal`: `getAffiliateGroup(pedestal.getGroup())` (pierwsza grupa w górę z nazwą „AFFILIATE …”) i `adoptionIsAllowedFor(grupa słupka)` (najbliższy atrybut adopcji w górę, brak = nie) `&& affiliateGroup != null && HOUSE_CARDS_ONLY == affiliateGroupService.checkAccessMode(affiliateGroup.getId())`. true → `houseCardService.saveHouseCard(owner, …, STAFF, …)` i karta powitalna skasowana (`deleted=true`); false → log, `return false`.
  - odpowiedź: `boolean` — tak: kemping pozwala przygarniać karty powitalne i przyjmuje tylko własne karty obsługi

###### Q12: Czy ten słupek ma pamiętać listę kart, które może przyjąć bez pytania centrali, i jakie karty ma na niej trzymać (karty obsługi, karty gości)?

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:124`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Osoba z obsługi technicznej podgląda, jakie karty ten słupek ma teraz na liście kart przyjmowanych bez pytania centrali (karty obsługi, karty gości) i ile kart się nie zmieściło. Jeśli w tej części sieci słupki nie trzymają listy, widzi pustą listę.
  - JAK: Wołający: `CardApiService.getLocalCardList` (`PEDESTAL_VIEW`, `findPedestalInScope`) → `localCardListService.buildLocalCardList(pedestalCode)` → `CardMapper.toDTO`. Metoda: `getPedestal` (brak/skasowany → `IllegalArgumentException`), `localListAttributeService.getLocalListAttribute(pedestal.getGroup())` (najbliższy atrybut LocalList, tylko gdy `localListAllowed`); null → `LocalCardList.empty(code, getCapacity(pedestal))`; inaczej prywatny `buildLocalCardList(pedestal, lla)`: karty domowe (`includeHouseCards`) + ważne karty wydawców akceptowanych na kempingu (`includeCards`, `groupService.findIssuerGroupIdsAcceptedAt`), przycięte do pojemności sterownika (`getCapacity` z typu słupka). Nic nie jest wysyłane.
  - odpowiedź: `LocalCardList` — lista: karty, które słupek przyjmie bez pytania centrali: najpierw karty obsługi, potem karty gości wydawców przyjmowanych na kempingu

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje od nowa całą listę kart przyjmowanych bez pytania centrali – na żądanie obsługi albo przy rozsyłce list do wszystkich słupków: jakie karty mają na niej być (karty obsługi, karty gości) i które wypadają, gdy brakuje miejsca? Karta, której nie ma na liście, wymaga pytania centrali.
  - JAK: Prywatny `buildLocalCardList(pedestal, lla)` woła `LocalCardListService.sendFull` (l. 81) po sprawdzeniu `getLocalListAttribute` (null → `return false`, bez budowania) oraz publiczna wersja z l. 124. `sendFull` wołają `CardApiService.sendLocalCardList` (`PEDESTAL_EDIT`) i `LocalCardListDistributionService.LocalCardListUpdater.run` (rozsyłka po skasowaniu starej listy). Budowa: `includeHouseCards` → `houseCardService.getHouseCardsForPedestal`; `includeCards` → `campingCardDao.findValidCardsForLocalCardList(groupService.findIssuerGroupIdsAcceptedAt(grupa słupka), dziś)`; ponad pojemność: gdy same karty domowe > pojemność → karty gości zerowane i `sortAndCutHouseCards`, inaczej `cutCards` do reszty miejsca; log ostrzeżenia z liczbą odrzuconych. W `sendFull`: `capacity()==0` → `false`; inaczej `localCardListSender.sendLocalList(pedestal, fullUpdate=true, entries, nextCardListVersion, useCache)`, nieudana wysyłka → `LocalCardListException`.
  - odpowiedź: `LocalCardList` — lista: karty, które słupek ma przyjmować bez pytania centrali: karty obsługi (ostatnio używane najpierw), potem karty gości wydawców przyjmowanych na kempingu (najnowsze najpierw)

- **LocalCardListService.updateLocalListWith** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:205`) · caller · ścieżka: LocalCardListService.updateLocalListWith → HouseCardService.findPedestalsForHouseCard · pyta: yes
  - CO: Karta obsługi kempingu została dodana, zmieniona albo wyłączona: które słupki trzymają ją na swojej liście kart przyjmowanych bez pytania centrali i muszą dostać tę zmianę? Słupek pominięty dalej przyjmuje (albo odrzuca) tę kartę po staremu.
  - JAK: Łańcuch: `HouseCardService.triggerLocalListUpdate` (po zapisie/zmianie/usunięciu karty domowej, l. 152, 167, 286) → `HouseCardChangedEvent` → `HouseCardChangedListener.handleEvent` → `updateLocalListWith(houseCardId)`. Ta woła `houseCardService.findPedestalsForHouseCard(houseCard)`: najbliższy atrybut LocalList liczony dla grupy KARTY (`houseCard.getGroup()`), null albo `!includeHouseCards` → pusta lista; inaczej słupki z `pedestalDao.findByGroupIds(groupService.findManageable(grupa karty))` filtrowane `isLocalListPedestal` (typ z listą + własny najbliższy atrybut pozwala na listę – bez ponownego sprawdzenia `includeHouseCards` na poziomie słupka); każdy wyjątek → pusta lista. Dla każdego słupka `sendDifferential(code, List.of(houseCard), true)`, błąd → log i dalej.
  - odpowiedź: `List<Pedestal>` — lista: słupki na kempingach właściciela karty (i niżej w sieci), które trzymają listę kart

- **LocalListAttributeService.findNearest** (`campsite-core/src/main/java/io/pillopl/group/attribute/LocalListAttributeService.java:70`) · top-in-island · ścieżka: LocalListAttributeService.findNearest · pyta: yes
  - CO: Czy w tej części sieci (kemping, region, rynek) słupki mają pamiętać listę kart przyjmowanych bez pytania centrali i czy trzymać na niej karty obsługi, karty gości, czy oba rodzaje? Rozstrzyga najbliższe ustawienie nad kempingiem; bez niego słupki listy nie trzymają.
  - JAK: Prywatny `findNearest(groupId)`: `groupService.fetchParentLineIds(groupId, true)` (grupa i jej przodkowie), `localListAttributeDao.findByGroupIds(linia)`, `GroupAttributeHelper.findNearest` – pierwszy atrybut idąc od grupy w górę. Woła go `isLocalListGroup(Long)` (`nearest != null && localListAllowed`) oraz `getLocalListAttribute(Group)` (zwraca atrybut tylko gdy `localListAllowed`, inaczej null). Dalej: `GroupApiService.getGroupAttributes` (pole `localList` na ekranie), `isLocalListPedestal` (wybór słupków dla zmiany karty obsługi), `LocalCardListService.sendFull` / `buildLocalCardList` i `HouseCardService.findPedestalsForHouseCard` (flagi `includeCards`, `includeHouseCards`).
  - odpowiedź: `LocalListAttribute | null` — najbliższe ustawienie listy kart nad kempingiem słupka

###### Q15: Czy karty tego wydawcy wolno wymieniać z tym partnerem — wysyłać mu je i przyjmować od niego?

- **IssuerExchangeService.exportCards** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:198`) · cited · ścieżka: IssuerExchangeService.exportCards · pyta: yes
  - CO: Przy wymianie z partnerami sieć wysyła każdemu partnerowi zmiany na liście swoich kart: czy kartę tego gościa (z tej części sieci) wolno przekazać właśnie temu partnerowi? Jeśli nie, partner jej nie dostaje i gość nie naładuje na kempingach partnera.
  - JAK: `IssuerExchangeService.exportCards` bierze karty sieci czekające na eksport (`campingCardDao.findCardsForPartnerExport(networkCardGroup)`), dla każdego partnera z eksportem i każdej karty woła `WhitelistFilterService.isSynchronizableForExport(grupa gościa karty, partner)`: najbliższy na linii rodziców filtr eksportowy, który celuje w tego partnera albo we wszystkich (`isTargeting`), decyduje; brak filtra = tak. true → `toCardLine` na liście partnera, false → karta pominięta tylko dla tego partnera. Lista (niepusta) idzie przez `sendCards`; status kierunku CARD_EXPORT zapisany. Karty oznaczane jako wyeksportowane tylko, gdy wszyscy partnerzy dostali listę (także te pominięte przez filtr).
  - odpowiedź: `boolean` — tak: kartę wolno wysłać temu partnerowi — nie ma ustalenia, które tego zabrania (albo najbliższe pozwala)

- **IssuerCardImportService.checkWhitelistSynchronizable** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:149`) · top-in-island · ścieżka: IssuerCardImportService.checkWhitelistSynchronizable · pyta: yes
  - CO: Partner przysyła zmiany na liście swoich kart: czy od tego partnera-wydawcy w ogóle wolno przyjmować karty? Jeśli nie, żadna jego karta nie zostaje przyjęta i jego goście nie naładują na kempingach sieci.
  - JAK: `IssuerCardImportService.importCard` jako pierwszy krok woła prywatne `checkWhitelistSynchronizable(partner)`: dla grupy wydawcy partnera (`partner.getIssuerGroup()`) pyta `WhitelistFilterService.isSynchronizableForImport(groupId, partner)` (najbliższy filtr importowy na linii rodziców celujący w partnera albo we wszystkich; brak = tak). false → `IssuerExchangeException("Issuer '…' is marked as not synchronizable")`. Sprawdzenie idzie dla każdej linii osobno, więc odrzucona jest każda linia z tym samym komunikatem; `importCards` liczy je jako odrzucenia. Partner bez grupy wydawcy — sprawdzenia nie ma.
  - odpowiedź: `void, throws IssuerExchangeException` — nic nie zwraca

- **WhitelistFilterService.findNearest** (`campsite-core/src/main/java/io/pillopl/group/attribute/WhitelistFilterService.java:110`) · top-in-island · ścieżka: WhitelistFilterService.findNearest · pyta: partly
  - CO: Dla danej części sieci (wydawcy): które ustalenie o wymianie kart z partnerami obowiązuje — najbliższe, idąc w górę od tej części, dla danego kierunku i partnera? Samo w sobie nie decyduje „tak/nie”; odpowiada na to, które ustalenie ma głos.
  - JAK: Prywatne `findNearest(issuerGroupId, applies)`: `groupService.fetchParentLineIds(id, true)` (od liścia w górę), potem `GroupAttributeHelper.findNearest(linia, whitelistFilterDao.findByGroupIds(linia), applies)` — pierwszy filtr na linii spełniający predykat (kierunek importu/eksportu i `isTargeting(partner)`). Wołają je cztery publiczne metody: `isSynchronizableForImport/Export(id, partner)` (używane przez import i eksport kart) oraz `getWhitelistFilterForImport`/`exportFilterOf` i jednoargumentowe `isSynchronizableForImport/Export(id)` — te ostatnie bez wywołań w kodzie produkcyjnym (grep).
  - odpowiedź: `WhitelistFilter | null` — ustalenie o wymianie kart, które obowiązuje dla tej części sieci w danym kierunku i dla tego partnera

###### Q16: Czy to karta sieci, czy karta wydana przez partnera — i czy przez tego partnera, który ją teraz przysyła?

- **IssuerExchangeService.importCards** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:139`) · caller · ścieżka: IssuerExchangeService.importCards → IssuerCardImportService.importCard · pyta: yes
  - CO: Partner przysyła zmiany na liście swoich kart: czy ta karta to karta sieci albo karta innego wydawcy, której ten partner nie może przejąć ani zmienić? Jeśli tak, ta jedna zmiana jest odrzucona, pozostałe przechodzą.
  - JAK: `IssuerExchangeService.importCards` pobiera linie (`fetchCards`) i dla każdej woła `IssuerCardImportService.importCard(partner, line)`. W środku: karta o tym UID istnieje i `isNetworkCard` → `IssuerExchangeException("CARD_UID='…' exists as a network card already")`; ADD/MODIFY/DELETE na karcie wydawcy (`isIssuerCard`) → `checkIssuer`: grupa gościa karty nie w `findManageable(grupa wydawcy partnera)` → `"…was imported from another issuer already"`; MODIFY/DELETE nieznanej → 'card not found'; ADD nieznanej → nowa karta na gościu partnera. `importCards` łapie każdy `RuntimeException`, liczy `accepted`/`rejections`, zwrot `importCard` ignoruje.
  - odpowiedź: `CampingCard` — nic nie zwraca

- **IssuerCardImportService.checkIssuer** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:160`) · top-in-island · ścieżka: IssuerCardImportService.checkIssuer · pyta: yes
  - CO: Partner przysyła dodanie, zmianę albo usunięcie karty, którą znamy jako kartę wydawcy: czy wydał ją właśnie ten partner? Jeśli nie, zmiana jest odrzucona — karta zostaje przy wydawcy, od którego przyszła wcześniej.
  - JAK: `importCard` woła `checkIssuer(partner, card, uid)` w gałęziach ADD/MODIFY/DELETE, gdy `isIssuerCard(card)`. `checkIssuer`: `groupService.findManageable(partner.getIssuerGroup())` (poddrzewo grupy wydawcy partnera) i `contains(card.getGuest().getGroup().getId())`; brak → `IssuerExchangeException("CARD_UID='…' was imported from another issuer already")`. Uwaga: przesunięcie grupy gościa w drzewie zmienia odpowiedź.
  - odpowiedź: `void, throws IssuerExchangeException` — nic nie zwraca

- **IssuerCardImportService.isNetworkCard** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:187`) · top-in-island · ścieżka: IssuerCardImportService.isNetworkCard · pyta: yes
  - CO: Partner przysyła kartę, którą już znamy: czy to karta sieci? Jeśli tak, nigdy nie przechodzi do wydawcy — zmiana jest odrzucona.
  - JAK: Jedyny wołający: `importCard:81` — `card != null && isNetworkCard(card)` → `IssuerExchangeException("CARD_UID='…' exists as a network card already")`. `isNetworkCard`: bez grupy gościa → false; inaczej `findNetworkCardGroupIds()` (poddrzewo grupy 'NETWORK CARD') zawiera grupę gościa karty.
  - odpowiedź: `boolean` — tak: posiadacz karty leży w części sieci kart sieciowych — to karta sieci

- **IssuerCardImportService.isIssuerCard** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:198`) · top-in-island · ścieżka: IssuerCardImportService.isIssuerCard · pyta: yes
  - CO: Partner przysyła zmianę karty, którą już znamy: czy to karta jakiegoś wydawcy (a nie sieci)? Jeśli tak, wolno ją aktualizować po sprawdzeniu, że to karta tego partnera; jeśli nie, dodanie zakłada nową kartę u partnera, a zmiana lub usunięcie jest odrzucone.
  - JAK: Wołane w `importCard` w trzech gałęziach (95, 110, 125) jako `card != null && isIssuerCard(card)`. `isIssuerCard`: bez grupy gościa → false; `findNetworkCardGroupIds()` null (brak 'NETWORK CARD') → true; inaczej grupa gościa poza poddrzewem kart sieci. ADD + true → `checkIssuer` i aktualizacja; ADD + false → `campingCardService.importCard` na gościu partnera; MODIFY/DELETE + false → 'card not found'.
  - odpowiedź: `boolean` — tak: karta jest kartą któregoś wydawcy

- **IssuerCardImportService.hasGuestGroup** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:206`) · top-in-island · ścieżka: IssuerCardImportService.hasGuestGroup · pyta: partly
  - CO: Czy znana karta ma posiadacza przypisanego do jakiejś części sieci albo wydawcy? Bez tego nie da się powiedzieć, czy to karta sieci, czy wydawcy — karta nie jest traktowana ani jako jedna, ani jako druga.
  - JAK: Prywatne statyczne `hasGuestGroup(card)`: `card != null && card.getGuest() != null && card.getGuest().getGroup() != null`. Wołane tylko z `isNetworkCard` i `isIssuerCard` jako strażnik; false → obie zwracają false.
  - odpowiedź: `boolean` — tak: karta ma posiadacza w strukturze sieci

- **IssuerCardImportService.findNetworkCardGroupIds** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:213`) · top-in-island · ścieżka: IssuerCardImportService.findNetworkCardGroupIds · pyta: partly
  - CO: Które części sieci należą do kart sieci (wydanych przez samą sieć)? Od tego zależy, czy przysłaną kartę uznać za kartę sieci, czy wydawcy.
  - JAK: Prywatne `findNetworkCardGroupIds()`: `groupService.fetchNetworkCardIssuerGroup()` (grupa ISSUER o nazwie 'NETWORK CARD'), jeśli jest → `groupService.findManageable(grupa)` (całe poddrzewo), inaczej null. Wołane przy każdym `isNetworkCard`/`isIssuerCard` — czyli kilka razy na linię.
  - odpowiedź: `List<Long> | null` — lista: części sieci (grupy wydawcy) należące do kart sieci — sama grupa kart sieci i wszystko pod nią

###### Q20: Jak jest zorganizowana sieć, za którą odpowiadam: które kempingi należą do których regionów i rynków (a wydawcy i zespoły — do kogo)?

- **GroupTreeFactory.createTree** (`campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:25`) · cited · ścieżka: GroupTreeFactory.createTree · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: Jedyny wołający: `GroupApiService.getGroupTree` (l.129) z grupą zakresu użytkownika dla danego typu. `createTree(root)`: null → null; inaczej `createNode(root, root.getType(), visited, 0)` — drzewo tylko z grup tego samego typu co korzeń, do 20 poziomów.
  - odpowiedź: `GroupTreeNodeDTO | null` — korzeń: część sieci, za którą odpowiada osoba, z całą strukturą pod nią

- **GroupTreeFactory.createNode** (`campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:32`) · cited · ścieżka: GroupTreeFactory.createNode · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: Rekurencyjne `createNode(group, type, visited, depth)`: dodaje id do `visited`; jeśli `depth < MAX_DEPTH (20)`, dla każdego dziecka tego samego typu i nieodwiedzonego woła się rekurencyjnie; zwraca `GroupTreeNodeDTO(id, name, displayName, type, children)`. Wołane z `createTree` i z siebie.
  - odpowiedź: `GroupTreeNodeDTO` — jedna część sieci (kemping, region, rynek albo wydawca, zespół) z tym, co pod nią

- **GroupWebService.getGroupTree** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:32`) · cited · wejście HTTP · ścieżka: GroupWebService.getGroupTree · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: HTTP POST `/getGroupTree` → `getBean().getGroupTree(caller(), request.groupType())` = `GroupApiService.getGroupTree`: `parseEnum(GroupType)` (puste → InvalidInputException), `principal.getGroup(type)` — brak zakresu → `ObjectNotFoundException("User … has no … scope")`; inaczej `GroupTreeFactory.createTree(scope)`. Wynik owinięty w `GetGroupTreeResponse`.
  - odpowiedź: `GetGroupTreeResponse` — część sieci, za którą odpowiada osoba, ze strukturą pod nią


### Implementacja referencyjna

Brak referencyjnej implementacji w paczce skilli — implementacja powstanie od zera według tej specyfikacji.
